/*
 * Copyright (c) 2018 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hidroh.materialistic.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.CursorWrapper
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.hidroh.materialistic.DataModule
import io.github.hidroh.materialistic.FavoriteActivity
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.ktx.closeQuietly
import io.github.hidroh.materialistic.ktx.getUri
import io.github.hidroh.materialistic.ktx.setChannel
import io.github.hidroh.materialistic.ktx.toSendIntentChooser
import okio.Okio
import rx.Observable
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Data repository for {@link Favorite}
 */
@Singleton
class FavoriteManager @Inject constructor(
    private val cache: LocalCache,
    @Named(DataModule.IO_THREAD) private val ioScheduler: Scheduler,
    private val dao: MaterialisticDatabase.SavedStoriesDao) : LocalItemManager<Favorite> {

  companion object {
    private const val CHANNEL_EXPORT = "export"
    private const val URI_PATH_ADD = "add"
    private const val URI_PATH_REMOVE = "remove"
    private const val URI_PATH_CLEAR = "clear"
    private const val PATH_SAVED = "saved"
    private const val FILENAME_EXPORT = "materialistic-export.txt"
    private const val FILE_AUTHORITY = "io.github.hidroh.materialistic.fileprovider"

    fun isAdded(uri: Uri) = uri.toString().startsWith(buildAdded().toString())

    fun isRemoved(uri: Uri) = uri.toString().startsWith(buildRemoved().toString())

    fun isCleared(uri: Uri) = uri.toString().startsWith(buildCleared().toString())

    private fun buildAdded(): Uri.Builder =
        MaterialisticDatabase.getBaseSavedUri().buildUpon().appendPath(URI_PATH_ADD)

    private fun buildCleared(): Uri.Builder =
        MaterialisticDatabase.getBaseSavedUri().buildUpon().appendPath(URI_PATH_CLEAR)

    private fun buildRemoved(): Uri.Builder =
        MaterialisticDatabase.getBaseSavedUri().buildUpon().appendPath(URI_PATH_REMOVE)
  }

  private val notificationId = System.currentTimeMillis().toInt()
  private val syncScheduler = SyncScheduler()
  private var cursor: Cursor? = null
  private var loader: FavoriteRoomLoader? = null

  override fun getSize() = cursor?.count ?: 0

  override fun getItem(position: Int) = if (cursor?.moveToPosition(position) == true) {
      cursor!!.favorite
    } else {
      null
    }

  override fun attach(observer: LocalItemManager.Observer, filter: String?) {
    loader = FavoriteRoomLoader(filter, observer)
    loader!!.load()
  }

  override fun detach() {
    if (cursor != null) {
      cursor = null
    }
    loader = null
  }

  /**
   * Exports all favorites matched given query to file
   * @param context   an instance of {@link android.content.Context}
   * @param query     query to filter stories to be retrieved
   */
  fun export(context: Context, query: String?) {
    val appContext = context.applicationContext
    notifyExportStart(appContext)
    Observable.defer { Observable.just(query) }
        .map { query(it) }
        .filter { it != null && it.moveToFirst() }
        .map {
          try {
            toFile(appContext, Cursor(it))
          } catch (e: IOException) {
            null
          } finally {
            it.close()
          }
        }
        .onErrorReturn { null }
        .defaultIfEmpty(null)
        .subscribeOn(ioScheduler)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { notifyExportDone(appContext, it) }
  }

  /**
   * Adds given story as favorite
   * @param context   an instance of {@link android.content.Context}
   * @param story     story to be added as favorite
   */
  fun add(context: Context, story: WebItem) {
    Observable.defer { Observable.just(story) }
        .doOnNext { insert(it) }
        .map { it.id }
        .map { buildAdded().appendPath(story.id).build() }
        .subscribeOn(ioScheduler)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { MaterialisticDatabase.getInstance(context).setLiveValue(it) }
    syncScheduler.scheduleSync(context, story.id)
  }

  /**
   * Clears all stories matched given query from favorites
   * will be sent upon completion
   * @param context   an instance of {@link android.content.Context}
   * @param query     query to filter stories to be cleared
   */
  fun clear(context: Context, query: String?) {
    Observable.defer { Observable.just(query) }
        .map { deleteMultiple(it) }
        .subscribeOn(ioScheduler)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { MaterialisticDatabase.getInstance(context).setLiveValue(buildCleared().build()) }
  }

  /**
   * Removes story with given ID from favorites
   * upon completion
   * @param context   an instance of {@link android.content.Context}
   * @param itemId    story ID to be removed from favorites
   */
  fun remove(context: Context, itemId: String?) {
    if (itemId == null) return
    Observable.defer { Observable.just(itemId) }
        .doOnNext { delete(it) }
        .map { buildRemoved().appendPath(it).build() }
        .subscribeOn(ioScheduler)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { MaterialisticDatabase.getInstance(context).setLiveValue(it) }
  }

  /**
   * Removes multiple stories with given IDs from favorites
   * be sent upon completion
   * @param context   an instance of {@link android.content.Context}
   * @param itemIds   array of story IDs to be removed from favorites
   */
  fun remove(context: Context, itemIds: Collection<String>?) {
    if (itemIds.orEmpty().isEmpty()) return
    Observable.defer { Observable.from(itemIds) }
        .subscribeOn(ioScheduler)
        .doOnNext { delete(it) }
        .map { buildRemoved().appendPath(it).build() }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { MaterialisticDatabase.getInstance(context).setLiveValue(it) }
  }

  @WorkerThread
  fun check(itemId: String?) = Observable.just(if (itemId.isNullOrEmpty()) {
    false
  } else {
    cache.isFavorite(itemId)
  })!!

  @WorkerThread
  private fun toFile(context: Context, cursor: Cursor): Uri? {
    if (cursor.count == 0) return null
    val dir = File(context.filesDir, PATH_SAVED)
    if (!dir.exists() && !dir.mkdir()) return null
    val file = File(dir, FILENAME_EXPORT)
    if (!file.exists() && !file.createNewFile()) return null
    val bufferedSink = Okio.buffer(Okio.sink(file))
    with(bufferedSink) {
      do {
        val item = cursor.favorite
        writeUtf8(item.displayedTitle)
        writeByte('\n'.toInt())
        writeUtf8(item.url)
        writeByte('\n'.toInt())
        writeUtf8(HackerNewsClient.WEB_ITEM_PATH.format(item.id))
        if (!cursor.isLast) {
          writeByte('\n'.toInt())
          writeByte('\n'.toInt())
        }
      } while (cursor.moveToNext())
      flush()
      closeQuietly()
    }
    return file.getUri(context, FILE_AUTHORITY)
  }

  private fun notifyExportStart(context: Context) {
    NotificationManagerCompat.from(context)
        .notify(notificationId, createNotificationBuilder(context)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(0, 0, true)
            .setContentIntent(PendingIntent.getActivity(context, 0,
                Intent(context, FavoriteActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT))
            .build())
  }

  private fun notifyExportDone(context: Context, uri: Uri?) {
    val manager = NotificationManagerCompat.from(context)
    with(manager) {
      cancel(notificationId)
      if (uri == null) return
      context.grantUriPermission(context.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
      notify(notificationId, createNotificationBuilder(context)
          .setPriority(NotificationCompat.PRIORITY_HIGH)
          .setVibrate(longArrayOf(0L))
          .setContentText(context.getString(R.string.export_notification))
          .setContentIntent(PendingIntent.getActivity(context, 0,
              uri.toSendIntentChooser(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
              PendingIntent.FLAG_UPDATE_CURRENT))
          .build())
    }
  }

  private fun createNotificationBuilder(context: Context) = NotificationCompat.Builder(context, CHANNEL_EXPORT)
      .setChannel(context, CHANNEL_EXPORT, context.getString(R.string.export_saved_stories))
      .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(context.getString(R.string.export_saved_stories))
      .setAutoCancel(true)!!

  @WorkerThread
  private fun query(filter: String?): android.database.Cursor = if (filter.isNullOrEmpty()) {
    dao.selectAllToCursor()
  } else {
    dao.searchToCursor(filter)
  }

  @WorkerThread
  private fun insert(story: WebItem) {
    dao.insert(MaterialisticDatabase.SavedStory.from(story))
    loader?.load()
  }

  @WorkerThread
  private fun delete(itemId: String?) {
    dao.deleteByItemId(itemId)
    loader?.load()
  }

  @WorkerThread
  private fun deleteMultiple(query: String?): Int {
    val deleted = if (query.isNullOrEmpty()) dao.deleteAll() else dao.deleteByTitle(query)
    loader?.load()
    return deleted
  }

  /**
   * A cursor wrapper to retrieve associated {@link Favorite}
   */
  private class Cursor(cursor: android.database.Cursor) : CursorWrapper(cursor) {
    val favorite: Favorite
      get() = Favorite(
          getString(getColumnIndexOrThrow(MaterialisticDatabase.FavoriteEntry.COLUMN_NAME_ITEM_ID)),
          getString(getColumnIndexOrThrow(MaterialisticDatabase.FavoriteEntry.COLUMN_NAME_URL)),
          getString(getColumnIndex(MaterialisticDatabase.FavoriteEntry.COLUMN_NAME_TITLE)),
          getString(getColumnIndex(MaterialisticDatabase.FavoriteEntry.COLUMN_NAME_TIME)).toLong())
  }

  inner class FavoriteRoomLoader(private val filter: String?,
                                 private val observer: LocalItemManager.Observer) {
    @AnyThread
    fun load() {
      Observable.defer { Observable.just(filter) }
          .map { query(it) }
          .subscribeOn(ioScheduler)
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe {
            cursor = if (it == null) null else Cursor(it)
            observer.onChanged()
          }
    }
  }
}
