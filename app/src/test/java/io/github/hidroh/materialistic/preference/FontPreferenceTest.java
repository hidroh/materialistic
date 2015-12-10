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

import io.github.hidroh.materialistic.Application;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.SettingsActivity;
import io.github.hidroh.materialistic.test.ParameterizedRobolectricGradleTestRunner;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(ParameterizedRobolectricGradleTestRunner.class)
public class FontPreferenceTest {
    private final int selection;
    private SettingsActivity activity;
    private View preferenceView;

    @ParameterizedRobolectricGradleTestRunner.Parameters
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
        activity = Robolectric.buildActivity(SettingsActivity.class)
                .create().postCreate(null).start().resume().visible().get();
        RecyclerView list = (RecyclerView) activity.findViewById(R.id.list);
        RecyclerView.Adapter adapter = list.getAdapter();
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(list, adapter.getItemViewType(2));
        adapter.onBindViewHolder(holder, 2);
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
}
