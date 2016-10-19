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

package io.github.hidroh.materialistic;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ReleaseNotesActivity extends InjectableActivity {

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_release);
        findViewById(R.id.button_ok).setOnClickListener(v -> finish());
        findViewById(R.id.button_rate).setOnClickListener(v -> {
            AppUtils.openPlayStore(this);
            finish();
        });
        WebView webView = (WebView) findViewById(R.id.web_view);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.loadDataWithBaseURL(null, getString(R.string.release_notes,
                AppUtils.toHtmlColor(this, android.R.attr.textColorPrimary),
                AppUtils.toHtmlColor(this, android.R.attr.textColorLink)),
                "text/html", "UTF-8", null);
        Preferences.setReleaseNotesSeen(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_down);
    }

    @Override
    protected boolean isDialogTheme() {
        return true;
    }
}
