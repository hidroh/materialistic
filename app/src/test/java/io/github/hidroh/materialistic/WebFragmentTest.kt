package io.github.hidroh.materialistic

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Base64
import android.view.View
import android.webkit.WebView
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ViewSwitcher
import androidx.core.widget.NestedScrollView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.hidroh.materialistic.WebFragment.PdfAndroidJavascriptBridge
import io.github.hidroh.materialistic.data.FavoriteManager
import io.github.hidroh.materialistic.data.FileDownloader.FileDownloaderCallback
import io.github.hidroh.materialistic.data.Item
import io.github.hidroh.materialistic.data.ReadabilityClient
import io.github.hidroh.materialistic.data.WebItem
import io.github.hidroh.materialistic.test.TestRunner
import io.github.hidroh.materialistic.test.WebActivity
import io.github.hidroh.materialistic.test.shadow.CustomShadows
import io.github.hidroh.materialistic.test.shadow.ShadowNestedScrollView
import io.github.hidroh.materialistic.test.shadow.ShadowWebView
import junit.framework.TestCase.*
import okio.buffer
import okio.source
import org.assertj.android.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowNetworkInfo
import org.robolectric.shadows.ShadowPopupMenu
import org.robolectric.shadows.ShadowToast
import java.io.File
import java.io.IOException
import javax.inject.Inject

@Config(shadows = [ShadowWebView::class])
@RunWith(TestRunner::class)
class WebFragmentTest {
    private var activity: WebActivity? = null
    private var controller: ActivityController<WebActivity>? = null
    private var item: WebItem? = null

    @JvmField
    @Inject
    var favoriteManager: FavoriteManager? = null

    @JvmField
    @Inject
    var readabilityClient: ReadabilityClient? = null
    private var intent: Intent? = null

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        TestApplication.applicationGraph.inject(this)
        Mockito.reset(favoriteManager)
        item = Mockito.mock(WebItem::class.java)
        Mockito.`when`(item!!.type).thenReturn(Item.STORY_TYPE)
        Mockito.`when`(item!!.url).thenReturn("http://example.com")
        intent = Intent()
        intent!!.putExtra(WebActivity.EXTRA_ITEM, item)
        controller = Robolectric.buildActivity(WebActivity::class.java, intent)
        Shadows.shadowOf(RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, NetworkInfo.State.CONNECTED))
        activity = controller!!.get()
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity!!.getString(R.string.pref_ad_block), true)
                .putBoolean(activity!!.getString(R.string.pref_lazy_load), false)
                .apply()
        controller!!.create().start().resume().visible()
    }

    @Test
    fun testProgressChanged() {
        val progressBar = activity!!.findViewById<ProgressBar>(R.id.progress)
        val webView = activity!!.findViewById<WebView>(R.id.web_view)
        Shadows.shadowOf(webView).webChromeClient.onProgressChanged(webView, 50)
        Assertions.assertThat(progressBar).isVisible
        Shadows.shadowOf(webView).webChromeClient.onProgressChanged(webView, 100)
        Assertions.assertThat(progressBar).isNotVisible
    }

    @Test
    fun testDownloadContent() {
        val resolverInfo = ResolveInfo()
        resolverInfo.activityInfo = ActivityInfo()
        resolverInfo.activityInfo.applicationInfo = ApplicationInfo()
        resolverInfo.activityInfo.applicationInfo.packageName = ListActivity::class.java.getPackage().name
        resolverInfo.activityInfo.name = ListActivity::class.java.name
        val rpm = Shadows.shadowOf(RuntimeEnvironment.application.packageManager)
        val url = "http://example.com/file.doc"
        rpm.addResolveInfoForIntent(Intent(Intent.ACTION_VIEW, Uri.parse(url)), resolverInfo)
        val webView = activity!!.findViewById<WebView>(R.id.web_view)
        val shadowWebView = Shadow.extract<ShadowWebView>(webView)
        Mockito.`when`(item!!.url).thenReturn(url)
        shadowWebView.downloadListener.onDownloadStart(url, "", "", "", 0L)
        Assertions.assertThat(activity!!.findViewById(R.id.empty) as View).isVisible
        activity!!.findViewById<View>(R.id.download_button).performClick()
        assertNotNull(Shadows.shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun testDownloadPdf() {
        val resolverInfo = ResolveInfo()
        resolverInfo.activityInfo = ActivityInfo()
        resolverInfo.activityInfo.applicationInfo = ApplicationInfo()
        resolverInfo.activityInfo.applicationInfo.packageName = ListActivity::class.java.getPackage().name
        resolverInfo.activityInfo.name = ListActivity::class.java.name
        val rpm = Shadows.shadowOf(RuntimeEnvironment.application.packageManager)
        Mockito.`when`(item!!.url).thenReturn("http://example.com/file.pdf")
        rpm.addResolveInfoForIntent(Intent(Intent.ACTION_VIEW, Uri.parse(item!!.url)), resolverInfo)
        val webView = activity!!.findViewById<WebView>(R.id.web_view)
        val shadowWebView = Shadow.extract<ShadowWebView>(webView)
        val fragment = activity!!.supportFragmentManager
                .findFragmentByTag(WebFragment::class.java.name) as WebFragment?
        shadowWebView.downloadListener.onDownloadStart(item!!.url, "", "", "application/pdf", 0L)
        shadowWebView.webViewClient.onPageFinished(webView, WebFragment.PDF_LOADER_URL)
        Mockito.verify(fragment!!.mFileDownloader).downloadFile(
                ArgumentMatchers.eq(item!!.url),
                ArgumentMatchers.eq("application/pdf"),
                ArgumentMatchers.any(FileDownloaderCallback::class.java))
    }

    @Test
    @Throws(IOException::class)
    fun testPdfAndroidJavascriptBridgeGetChunk() {
        val path = this.javaClass.classLoader.getResource("file.txt").path
        val file = File(path)
        val size = file.length()
        val expected = Base64.encodeToString(file.source().buffer().readByteArray(), Base64.DEFAULT)
        val bridge = PdfAndroidJavascriptBridge(path, null)
        assertEquals(expected, bridge.getChunk(0, size))
    }

    @Test
    fun testPdfAndroidJavascriptBridgeGetSize() {
        val path = this.javaClass.classLoader.getResource("file.txt").path
        val expected = File(path).length()
        val bridge = PdfAndroidJavascriptBridge(path, null)
        assertEquals(expected, bridge.size)
    }

    @Config(shadows = [ShadowNestedScrollView::class])
    @Test
    fun testScrollToTop() {
        val scrollView = activity!!.findViewById<NestedScrollView>(R.id.nested_scroll_view)
        scrollView.smoothScrollTo(0, 1)
        org.assertj.core.api.Assertions.assertThat(CustomShadows.customShadowOf(scrollView).smoothScrollY).isEqualTo(1)
        activity!!.fragment.scrollToTop()
        org.assertj.core.api.Assertions.assertThat(CustomShadows.customShadowOf(scrollView).smoothScrollY).isEqualTo(0)
    }

    @Test
    fun testFullscreenScrollToTop() {
        activity!!.findViewById<View>(R.id.toolbar_web).performClick()
        assertEquals(-1, (Shadow.extract<Any>(activity!!.findViewById(R.id.web_view)) as ShadowWebView)
                .scrollY)
    }

    @Test
    fun testFullscreen() {
        LocalBroadcastManager.getInstance(activity!!)
                .sendBroadcast(Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true))
        Assertions.assertThat(activity!!.findViewById(R.id.control_switcher) as View).isVisible
        activity!!.recreate()
        Assertions.assertThat(activity!!.findViewById(R.id.control_switcher) as View).isVisible
        activity!!.findViewById<View>(R.id.button_exit).performClick()
        Assertions.assertThat(activity!!.findViewById(R.id.control_switcher) as View).isNotVisible
    }

    @Test
    fun testSearch() {
        LocalBroadcastManager.getInstance(activity!!)
                .sendBroadcast(Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true))
        activity!!.findViewById<View>(R.id.button_find).performClick()
        val controlSwitcher = activity!!.findViewById<ViewSwitcher>(R.id.control_switcher)
        org.assertj.core.api.Assertions.assertThat(controlSwitcher.displayedChild).isEqualTo(1)
        val shadowWebView = Shadow.extract<ShadowWebView>(activity!!.findViewById(R.id.web_view))

        // no query
        val editText = activity!!.findViewById<EditText>(R.id.edittext)
        Shadows.shadowOf(editText).onEditorActionListener.onEditorAction(null, 0, null)
        Assertions.assertThat(activity!!.findViewById(R.id.button_next) as View).isDisabled

        // with results
        shadowWebView.setFindCount(1)
        editText.setText("abc")
        Shadows.shadowOf(editText).onEditorActionListener.onEditorAction(null, 0, null)
        Assertions.assertThat(activity!!.findViewById(R.id.button_next) as View).isEnabled
        activity!!.findViewById<View>(R.id.button_next).performClick()
        org.assertj.core.api.Assertions.assertThat(shadowWebView.findIndex).isEqualTo(1)
        activity!!.findViewById<View>(R.id.button_clear).performClick()
        Assertions.assertThat(editText).isEmpty
        org.assertj.core.api.Assertions.assertThat(controlSwitcher.displayedChild).isEqualTo(0)

        // with no results
        shadowWebView.setFindCount(0)
        editText.setText("abc")
        Shadows.shadowOf(editText).onEditorActionListener.onEditorAction(null, 0, null)
        Assertions.assertThat(activity!!.findViewById(R.id.button_next) as View).isDisabled
        org.assertj.core.api.Assertions.assertThat(ShadowToast.getTextOfLatestToast()).contains(activity!!.getString(R.string.no_matches))
    }

    @Test
    fun testRefresh() {
        LocalBroadcastManager.getInstance(activity!!)
                .sendBroadcast(Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true))
        ShadowWebView.lastGlobalLoadedUrl = null
        val shadowWebView = Shadow.extract<ShadowWebView>(activity!!.findViewById(R.id.web_view))
        shadowWebView.progress = 20
        activity!!.findViewById<View>(R.id.button_refresh).performClick()
        org.assertj.core.api.Assertions.assertThat(ShadowWebView.getLastGlobalLoadedUrl()).isNullOrEmpty()
        shadowWebView.progress = 100
        activity!!.findViewById<View>(R.id.button_refresh).performClick()
        org.assertj.core.api.Assertions.assertThat(ShadowWebView.getLastGlobalLoadedUrl()).isEqualTo(ShadowWebView.RELOADED)
    }

    @SuppressLint("NewApi")
    @Test
    fun testWebControls() {
        LocalBroadcastManager.getInstance(activity!!)
                .sendBroadcast(Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true))
        val shadowWebView = Shadow.extract<ShadowWebView>(activity!!.findViewById(R.id.web_view))
        activity!!.findViewById<View>(R.id.button_more).performClick()
        Shadows.shadowOf(ShadowPopupMenu.getLatestPopupMenu()).onMenuItemClickListener
                .onMenuItemClick(RoboMenuItem(R.id.menu_zoom_in))
        org.assertj.core.api.Assertions.assertThat(shadowWebView.zoomDegree).isEqualTo(1)
        activity!!.findViewById<View>(R.id.button_more).performClick()
        Shadows.shadowOf(ShadowPopupMenu.getLatestPopupMenu()).onMenuItemClickListener
                .onMenuItemClick(RoboMenuItem(R.id.menu_zoom_out))
        org.assertj.core.api.Assertions.assertThat(shadowWebView.zoomDegree).isEqualTo(0)
        activity!!.findViewById<View>(R.id.button_forward).performClick()
        org.assertj.core.api.Assertions.assertThat(shadowWebView.pageIndex).isEqualTo(1)
        activity!!.findViewById<View>(R.id.button_back).performClick()
        org.assertj.core.api.Assertions.assertThat(shadowWebView.pageIndex).isEqualTo(0)
    }

    @Config(shadows = [ShadowNestedScrollView::class])
    @Test
    fun testScroll() {
        val shadowScrollView = CustomShadows.customShadowOf(activity!!.findViewById<View>(R.id.nested_scroll_view) as NestedScrollView)
        val fragment = activity!!.supportFragmentManager
                .findFragmentByTag(WebFragment::class.java.name) as WebFragment?
        fragment!!.scrollToNext()
        org.assertj.core.api.Assertions.assertThat(shadowScrollView.lastScrollDirection).isEqualTo(View.FOCUS_DOWN)
        fragment.scrollToPrevious()
        org.assertj.core.api.Assertions.assertThat(shadowScrollView.lastScrollDirection).isEqualTo(View.FOCUS_UP)
        fragment.scrollToTop()
        org.assertj.core.api.Assertions.assertThat(shadowScrollView.smoothScrollY).isEqualTo(0)
    }

    @Test
    fun testFullScroll() {
        LocalBroadcastManager.getInstance(activity!!)
                .sendBroadcast(Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true))
        val shadowWebView = Shadow.extract<ShadowWebView>(activity!!.findViewById(R.id.web_view))
        val fragment = activity!!.supportFragmentManager
                .findFragmentByTag(WebFragment::class.java.name) as WebFragment?
        fragment!!.scrollToTop()
        assertEquals(0, shadowWebView.scrollY)
        fragment.scrollToNext()
        assertEquals(1, shadowWebView.scrollY)
        fragment.scrollToPrevious()
        assertEquals(0, shadowWebView.scrollY)
    }

    @Test
    fun testBackPressed() {
        val webView = activity!!.findViewById<WebView>(R.id.web_view)
        Shadows.shadowOf(webView).webViewClient.onPageFinished(webView, "http://example.com")
        Shadows.shadowOf(webView).setCanGoBack(true)
        assertTrue(activity!!.fragment.onBackPressed())
        Shadows.shadowOf(webView).setCanGoBack(false)
        assertFalse(activity!!.fragment.onBackPressed())
    }

    @Test
    fun testReadabilityToggle() {
        activity!!.fragment.onOptionsItemSelected(RoboMenuItem(R.id.menu_readability))
        Mockito.verify(readabilityClient)!!.parse(ArgumentMatchers.any(), ArgumentMatchers.eq("http://example.com"), ArgumentMatchers.any())
    }

    @After
    fun tearDown() {
        controller!!.pause().stop().destroy()
    }
}
