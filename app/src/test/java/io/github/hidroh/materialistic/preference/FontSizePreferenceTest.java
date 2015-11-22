package io.github.hidroh.materialistic.preference;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Spinner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.util.Arrays;
import java.util.List;

import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.SettingsActivity;
import io.github.hidroh.materialistic.test.ParameterizedRobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

@RunWith(ParameterizedRobolectricGradleTestRunner.class)
public class FontSizePreferenceTest {
    private final int selection;
    private final int styleResId;
    private SettingsActivity activity;
    private View preferenceView;

    @ParameterizedRobolectricGradleTestRunner.Parameters
    public static List<Object[]> provideParameters() {
        return Arrays.asList(
                new Object[]{0, R.style.AppTextSize_XSmall},
                new Object[]{1, R.style.AppTextSize},
                new Object[]{2, R.style.AppTextSize_Medium},
                new Object[]{3, R.style.AppTextSize_Large},
                new Object[]{4, R.style.AppTextSize_XLarge}
        );
    }

    public FontSizePreferenceTest(int selection, int styleResId) {
        this.selection = selection;
        this.styleResId = styleResId;
    }

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(SettingsActivity.class)
                .create().postCreate(null).start().resume().visible().get();
        shadowOf(activity.getTheme()).setTo(activity.getResources().newTheme());
        RecyclerView list = (RecyclerView) activity.findViewById(R.id.list);
        RecyclerView.Adapter adapter = list.getAdapter();
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(list, adapter.getItemViewType(2));
        adapter.onBindViewHolder(holder, 2);
        preferenceView = holder.itemView;
    }

    @Test
    public void test() {
        preferenceView.findViewById(R.id.spinner).performClick();
        ((Spinner) preferenceView.findViewById(R.id.spinner)).setSelection(selection);
        Preferences.Theme.apply(activity);
        assertEquals(styleResId, shadowOf(activity.getTheme()).getStyleResourceId());
    }
}
