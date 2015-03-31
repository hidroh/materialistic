package io.github.hidroh.materialistic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import static org.assertj.android.api.Assertions.assertThat;


@Config(emulateSdk = 21, reportSdk = 21)
@RunWith(RobolectricTestRunner.class)
public class SettingsActivityTest {
    @Test
    public void test() {
        ActivityController<SettingsActivity> controller = Robolectric.buildActivity(SettingsActivity.class);
        SettingsActivity activity = controller.create().get();
        assertThat(activity.getPreferenceScreen()).isNotNull();
        controller.destroy();
    }
}
