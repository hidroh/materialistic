/*
 * Copyright (c) 2016 Ha Duy Trung
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

package io.github.hidroh.materialistic

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class ReleaseNotesActivity : InjectableActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    setContentView(R.layout.activity_release)
    findViewById<View>(R.id.button_ok).setOnClickListener { _ -> finish() }
    findViewById<View>(R.id.button_rate).setOnClickListener { _ ->
      AppUtils.openPlayStore(this)
      finish()
    }
    with(findViewById<WebView>(R.id.web_view)) {
      webViewClient = WebViewClient()
      webChromeClient = WebChromeClient()
      setBackgroundColor(Color.TRANSPARENT)
      loadDataWithBaseURL(null, getString(R.string.release_notes,
          AppUtils.toHtmlColor(this@ReleaseNotesActivity, android.R.attr.textColorPrimary),
          AppUtils.toHtmlColor(this@ReleaseNotesActivity, android.R.attr.textColorLink)),
          "text/html", "UTF-8", null)
    }
    Preferences.setReleaseNotesSeen(this)
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, R.anim.slide_out_down)
  }

  override fun isDialogTheme() = true
}
