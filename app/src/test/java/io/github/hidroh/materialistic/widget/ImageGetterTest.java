package io.github.hidroh.materialistic.widget;

import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import javax.inject.Inject;

import io.github.hidroh.materialistic.ImageUtils;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.TestApplication;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class ImageGetterTest {
    @Inject ImageUtils imageUtils;
    private TextView textView;
    private TestImageGetter imageGetter;

    @Before
    public void setUp() {
        TestApplication.applicationGraph.inject(this);
        reset(imageUtils);
        textView = new TextView(RuntimeEnvironment.application);
        imageGetter = new TestImageGetter(textView);
    }

    @Test
    public void testBitmapFromUri() {
        when(imageUtils.fromUri(any(Uri.class))).thenReturn(BitmapFactory.decodeResource(
                RuntimeEnvironment.application.getResources(), R.drawable.ic_app));
        textView.setText(Html.fromHtml("<div><img src=\"http://example.com/image.png\" /></div>",
                imageGetter, null));
        verify(imageUtils).fromUri(eq(Uri.parse("http://example.com/image.png")));
        assertNotNull(imageGetter.lastDrawable);
    }

    @Test
    public void testNullBitmap() {
        textView.setText(Html.fromHtml("<div><img src=\"http://example.com/image.png\" /></div>",
                imageGetter, null));
        verify(imageUtils).fromUri(eq(Uri.parse("http://example.com/image.png")));
    }

    @Test
    public void testInvalidUri() {
        textView.setText(Html.fromHtml("<div><img src=\"/image.png\" /></div>",
                imageGetter, null));
        verify(imageUtils, never()).fromUri(any(Uri.class));
    }

    @Test
    public void testNoImg() {
        textView.setText(Html.fromHtml("<div></div>", imageGetter, null));
        verify(imageUtils, never()).fromUri(any(Uri.class));
    }

    public static class TestImageGetter extends ImageGetter {
        private Drawable lastDrawable;

        public TestImageGetter(TextView view) {
            super(view);
        }

        @Override
        public Drawable getDrawable(String source) {
            lastDrawable = super.getDrawable(source);
            return lastDrawable;
        }
    }
}
