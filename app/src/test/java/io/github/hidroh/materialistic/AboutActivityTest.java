package io.github.hidroh.materialistic;

import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import io.github.hidroh.materialistic.test.TestRunner;

import static org.assertj.android.api.Assertions.assertThat;

@RunWith(TestRunner.class)
public class AboutActivityTest {
    @Test
    public void test() {
        AboutActivity activity = Robolectric.setupActivity(AboutActivity.class);
        assertThat((TextView) activity.findViewById(R.id.text_application_info)).containsText("Version");
        assertThat((TextView) activity.findViewById(R.id.text_developer_info)).containsText("Ha Duy Trung");
        assertThat((TextView) activity.findViewById(R.id.text_libraries)).isNotEmpty();
        assertThat((TextView) activity.findViewById(R.id.text_license)).containsText("Apache");
    }
}
