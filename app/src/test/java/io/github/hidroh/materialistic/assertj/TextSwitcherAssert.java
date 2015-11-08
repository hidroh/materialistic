package io.github.hidroh.materialistic.assertj;

import android.support.v4.content.ContextCompat;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.assertj.android.api.Assertions;
import org.assertj.android.api.widget.ViewSwitcherAssert;

public class TextSwitcherAssert extends ViewSwitcherAssert {
    public static TextSwitcherAssert assertThat(TextSwitcher actual) {
        return new TextSwitcherAssert(actual);
    }

    public TextSwitcherAssert(ViewSwitcher actual) {
        super(actual);
    }

    public TextSwitcherAssert hasCurrentTextString(String text) {
        isNotNull();
        TextView actualTextView = (TextView) actual.getCurrentView();
        Assertions.assertThat(actualTextView)
                .isNotNull()
                .hasTextString(text);
        return this;
    }

    public TextSwitcherAssert hasCurrentText(int resId) {
        isNotNull();
        return hasCurrentTextString(actual.getContext().getString(resId));
    }

    public TextSwitcherAssert hasCurrentTextColor(int colorResId) {
        isNotNull();
        TextView actualTextView = (TextView) actual.getCurrentView();
        Assertions.assertThat(actualTextView)
                .isNotNull()
                .hasCurrentTextColor(ContextCompat.getColor(actual.getContext(), colorResId));
        return this;
    }
}
