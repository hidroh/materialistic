package io.github.hidroh.materialistic;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.assertj.android.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class AppUtilsTest {
    @Test
    public void testMakeShareIntent() {
        Intent actual = AppUtils.makeShareIntent("content");
        assertThat(actual).hasAction(Intent.ACTION_SEND);
        assertThat(actual).hasType("text/plain");
        assertThat(actual).hasExtra(Intent.EXTRA_TEXT);
    }
}
