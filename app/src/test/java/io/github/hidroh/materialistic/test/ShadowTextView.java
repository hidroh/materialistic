package io.github.hidroh.materialistic.test;

import android.graphics.Typeface;
import android.widget.TextView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(TextView.class)
public class ShadowTextView extends org.robolectric.shadows.ShadowTextView {
    private Typeface typeface;

    @Implementation
    public void setTypeface(Typeface tf) {
        typeface = tf;
    }

    public Typeface getTypeface() {
        return typeface;
    }
}

