package io.github.hidroh.materialistic.preference;

import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Spinner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import java.util.Arrays;
import java.util.List;

import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.SettingsActivity;
import io.github.hidroh.materialistic.test.ParameterizedRobolectricGradleTestRunner;
import io.github.hidroh.materialistic.test.ShadowSupportPreference;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;

import static org.assertj.core.api.Assertions.assertThat;

@Config(shadows = {ShadowSupportPreferenceManager.class, ShadowSupportPreference.class})
@RunWith(ParameterizedRobolectricGradleTestRunner.class)
public class FontSizePreferenceTest {
    private final int selection;
    private final int styleResId;
    private ActivityController<SettingsActivity> controller;
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
        controller = Robolectric.buildActivity(SettingsActivity.class);
        activity = controller.create().postCreate(null).start().resume().visible().get();
        RecyclerView list = (RecyclerView) activity.findViewById(R.id.list);
        RecyclerView.Adapter adapter = list.getAdapter();
        int position = ShadowSupportPreferenceManager
                .getPreferencePosition((PreferenceGroupAdapter) adapter, FontSizePreference.class);
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(list,
                adapter.getItemViewType(position));
        adapter.onBindViewHolder(holder, position);
        preferenceView = holder.itemView;
    }

    @Test
    public void test() {
        preferenceView.performClick();
        ((Spinner) preferenceView.findViewById(R.id.spinner)).setSelection(selection);
        assertThat(Preferences.Theme.resolvePreferredTextSize(activity)).isEqualTo(styleResId);
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}
