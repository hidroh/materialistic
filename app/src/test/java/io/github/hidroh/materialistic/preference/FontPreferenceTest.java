package io.github.hidroh.materialistic.preference;

import android.content.Intent;
import androidx.preference.PreferenceGroupAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.Spinner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import io.github.hidroh.materialistic.Application;
import io.github.hidroh.materialistic.PreferencesActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.test.shadow.CustomShadows;
import io.github.hidroh.materialistic.test.shadow.ShadowPreferenceFragmentCompat;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerViewAdapter;
import io.github.hidroh.materialistic.test.shadow.ShadowSupportPreference;
import io.github.hidroh.materialistic.test.shadow.ShadowSupportPreferenceManager;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@Config(shadows = {ShadowSupportPreferenceManager.class, ShadowSupportPreference.class, ShadowPreferenceFragmentCompat.class, ShadowRecyclerViewAdapter.class})
@RunWith(ParameterizedRobolectricTestRunner.class)
public class FontPreferenceTest {
    private final int selection;
    private PreferencesActivity activity;
    private ActivityController<PreferencesActivity> controller;
    private View preferenceView;

    @ParameterizedRobolectricTestRunner.Parameters
    public static List<Object[]> provideParameters() {
        return Arrays.asList(
                new Object[]{1},
                new Object[]{2},
                new Object[]{3},
                new Object[]{4}
        );
    }

    public FontPreferenceTest(int selection) {
        this.selection = selection;
    }

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(PreferencesActivity.class,
                new Intent()
                        .putExtra(PreferencesActivity.EXTRA_TITLE, R.string.display)
                        .putExtra(PreferencesActivity.EXTRA_PREFERENCES, R.xml.preferences_display));
        activity = controller.create().postCreate(null).start().resume().visible().get();
        RecyclerView list = activity.findViewById(android.R.id.list_container);
        list.setLayoutManager(new LinearLayoutManager(activity));
        RecyclerView.Adapter adapter = list.getAdapter();
        int position = ShadowSupportPreferenceManager
                .getPreferencePosition((PreferenceGroupAdapter) adapter, FontPreference.class);
        RecyclerView.ViewHolder holder = CustomShadows.customShadowOf(adapter).getViewHolder(position);
        preferenceView = holder.itemView;
    }

    @Test
    public void test() {
        preferenceView.performClick();
        ((Spinner) preferenceView.findViewById(R.id.spinner)).setSelection(selection);
        assertNotNull(Application.TYPE_FACE);
        preferenceView.performClick();
        ((Spinner) preferenceView.findViewById(R.id.spinner)).setSelection(0);
        assertNull(Application.TYPE_FACE);
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}
