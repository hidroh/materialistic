package io.github.hidroh.materialistic;

import android.content.ContentValues;
import android.support.v7.widget.RecyclerView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.data.MaterialisticProvider;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class FavoriteActivityTest {
    private ActivityController<FavoriteActivity> controller;
    private FavoriteActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(FavoriteActivity.class);
        activity = controller.get();
    }

    @Test
    public void testEmpty() {
        controller.create().start().resume().get(); // skip menu due to search view
        assertThat(activity.findViewById(R.id.empty)).isVisible();
        activity.findViewById(R.id.header_card_view).performLongClick();
        assertThat(activity.findViewById(R.id.header_card_view)
                .findViewById(R.id.bookmarked)).isVisible();
        activity.findViewById(R.id.header_card_view).performLongClick();
        assertThat(activity.findViewById(R.id.header_card_view)
                .findViewById(R.id.bookmarked)).isNotVisible();
    }

    @Test
    public void testItemClick() {
        ShadowContentResolver resolver = shadowOf(ShadowApplication.getInstance().getContentResolver());
        ContentValues cv = new ContentValues();
        cv.put("itemid", "1");
        cv.put("title", "title");
        cv.put("url", "http://example.com");
        cv.put("time", String.valueOf(System.currentTimeMillis()));
        resolver.insert(MaterialisticProvider.URI_FAVORITE, cv);
        controller.create().start().resume().get(); // skip menu due to search view
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        assertEquals(1, adapter.getItemCount());
        RecyclerView.ViewHolder holder = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(holder, 0);
        holder.itemView.performClick();
        assertNotNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void testDataChanged() {
        controller.create().start().resume().get(); // skip menu due to search view
        assertThat(activity.findViewById(R.id.empty_search)).isNotVisible();
        activity.onDataChanged(true, "query");
        assertThat(activity.findViewById(R.id.empty_search)).isVisible();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}
