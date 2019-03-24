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

package io.github.hidroh.materialistic.ktx

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import io.github.hidroh.materialistic.AppUtils
import okhttp3.internal.Util
import java.io.Closeable
import java.io.File

inline fun Closeable.closeQuietly() = Util.closeQuietly(this)

inline fun File.getUri(context: Context, authority: String) =
    FileProvider.getUriForFile(context, authority, this)!!

inline fun Uri.toSendIntentChooser(context: Context) =
    AppUtils.makeSendIntentChooser(context, this)!!

fun NotificationCompat.Builder.setChannel(context: Context,
    channelId: String,
    name: CharSequence): NotificationCompat.Builder {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .createNotificationChannel(NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW))
    this.setChannelId(channelId)
  }
  return this
}
