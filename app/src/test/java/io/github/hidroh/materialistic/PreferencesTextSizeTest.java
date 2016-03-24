package io.github.hidroh.materialistic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import java.util.Arrays;
import java.util.List;

import io.github.hidroh.materialistic.test.ParameterizedRobolectricGradleTestRunner;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowSupportPreferenceManager.class})
@RunWith(ParameterizedRobolectricGradleTestRunner.class)
public class PreferencesTextSizeTest {
    private final String choice;
    private final int resId;
    private ActivityController<SettingsActivity> controller;
    private SettingsActivity activity;

    public PreferencesTextSizeTest(String choice, int resId) {
        this.choice = choice;
        this.resId = resId;
    }

    @ParameterizedRobolectricGradleTestRunner.Parameters
    public static List<Object[]> provideParameters() {
        return Arrays.asList(
                new Object[]{"-1", R.style.AppTextSize_XSmall},
                new Object[]{"1", R.style.AppTextSize_Medium},
                new Object[]{"2", R.style.AppTextSize_Large},
                new Object[]{"3", R.style.AppTextSize_XLarge}
        );
    }

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(SettingsActivity.class).create().start().resume();
        activity = controller.get();
    }

    @Test
    public void testResolveTextSizeResId() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_text_size), choice)
                .commit();
        shadowOf(activity).recreate();
        float expected = activity.getTheme().obtainStyledAttributes(resId,
                new int[]{R.attr.contentTextSize}).getDimension(0, 0);
        float actual = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.contentTextSize}).getDimension(0, 0);
        assertThat(actual).isEqualTo(expected);
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}
