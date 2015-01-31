package io.github.hidroh.materialistic.test;

import android.webkit.TestWebSettings;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(value = WebView.class, inheritImplementationMethods = true)
public class ShadowWebView extends org.robolectric.shadows.ShadowWebView {
    @Override @Implementation
    public WebSettings getSettings() {
        return new TestWebSettings() {
            @Override
            public void setDisplayZoomControls(boolean enabled) {
                // do nothing
            }

            @Override
            public void setMixedContentMode(int mode) {
                // do nothing
            }

            @Override
            public int getMixedContentMode() {
                return 0;
            }
        };
    }
}
