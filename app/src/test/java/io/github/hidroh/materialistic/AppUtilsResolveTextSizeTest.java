package io.github.hidroh.materialistic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPreferenceManager;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class AppUtilsResolveTextSizeTest {
    private final String choice;
    private final int resId;

    public AppUtilsResolveTextSizeTest(String choice, int resId) {
        this.choice = choice;
        this.resId = resId;
    }

    @ParameterizedRobolectricTestRunner.Parameters
    public static List<Object[]> provideParameters() {
        return Arrays.asList(
                new Object[]{"-1", R.style.AppTextSize_XSmall},
                new Object[]{"1", R.style.AppTextSize_Medium},
                new Object[]{"2", R.style.AppTextSize_Large},
                new Object[]{"3", R.style.AppTextSize_XLarge}
        );
    }

    @Test
    public void testResolveTextSizeResId() {
        ShadowPreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putString(RuntimeEnvironment.application.getString(R.string.pref_text_size), choice)
                .commit();
        assertEquals(resId, AppUtils.resolveTextSizeResId(RuntimeEnvironment.application));
    }
}
