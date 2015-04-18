package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Intent;
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
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class FavoriteActivityTest {
    private ActivityController<FavoriteActivity> controller;
    private FavoriteActivity activity;
    private RecyclerView.Adapter adapter;
    private RecyclerView recyclerView;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(FavoriteActivity.class);
        ShadowContentResolver resolver = shadowOf(ShadowApplication.getInstance().getContentResolver());
        ContentValues cv = new ContentValues();
        cv.put("itemid", "1");
        cv.put("title", "title");
        cv.put("url", "http://example.com");
        cv.put("time", String.valueOf(System.currentTimeMillis()));
        resolver.insert(MaterialisticProvider.URI_FAVORITE, cv);
        cv = new ContentValues();
        cv.put("itemid", "2");
        cv.put("title", "ask HN");
        cv.put("url", "http://example.com");
        cv.put("time", String.valueOf(System.currentTimeMillis()));
        resolver.insert(MaterialisticProvider.URI_FAVORITE, cv);
        activity = controller.create().start().resume().get(); // skip menu due to search view
        recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        adapter = recyclerView.getAdapter();
    }

    @Test
    public void testItemClick() {
        assertEquals(2, adapter.getItemCount());
        RecyclerView.ViewHolder holder = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(holder, 0);
        holder.itemView.performClick();
        assertNotNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void testFilter() {
        Intent intent = new Intent();
        intent.putExtra(SearchManager.QUERY, "blah");
        controller.newIntent(intent);
        assertEquals(0, adapter.getItemCount());
        intent = new Intent();
        intent.putExtra(SearchManager.QUERY, "ask");
        controller.newIntent(intent);
        assertEquals(1, adapter.getItemCount());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}
