package io.github.hidroh.materialistic.preference;

import android.support.v7.preference.Preference;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Spinner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;

import java.util.Arrays;
import java.util.List;

import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.SettingsActivity;
import io.github.hidroh.materialistic.SettingsFragment;
import io.github.hidroh.materialistic.test.ParameterizedRobolectricGradleTestRunner;
import io.github.hidroh.materialistic.test.ShadowSupportPreference;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;

import static junit.framework.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowSupportPreferenceManager.class, ShadowSupportPreference.class})
@RunWith(ParameterizedRobolectricGradleTestRunner.class)
public class CommentMaxLinesPreferenceTest {
    private final int selection;
    private final String maxLines;
    private SettingsActivity activity;
    private View preferenceView;
    private Preference preference;

    @ParameterizedRobolectricGradleTestRunner.Parameters
    public static List<Object[]> provideParameters() {
        return Arrays.asList(
                new Object[]{0, "3"},
                new Object[]{1, "4"},
                new Object[]{2, "5"},
                new Object[]{3, "-1"}
        );
    }

    public CommentMaxLinesPreferenceTest(int selection, String maxLines) {
        this.selection = selection;
        this.maxLines = maxLines;
    }

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(SettingsActivity.class)
                .create().postCreate(null).start().resume().visible().get();
        shadowOf(activity.getTheme()).setTo(activity.getResources().newTheme());
        RecyclerView list = (RecyclerView) activity.findViewById(R.id.list);
        RecyclerView.Adapter adapter = list.getAdapter();
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(list, adapter.getItemViewType(11));
        adapter.onBindViewHolder(holder, 11);
        preferenceView = holder.itemView;
        preference = ((SettingsFragment) activity
                .getSupportFragmentManager()
                .findFragmentByTag(SettingsFragment.class.getName()))
                .findPreference(activity.getString(R.string.pref_max_lines));
    }

    @Test
    public void test() {
        preferenceView.performClick();
        ((Spinner) preferenceView.findViewById(R.id.spinner)).setSelection(selection);
        assertEquals(maxLines, ((ShadowSupportPreference) ShadowExtractor.extract(preference))
                .getShadowPersistedString());
    }
}
