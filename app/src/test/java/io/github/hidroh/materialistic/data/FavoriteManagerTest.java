package io.github.hidroh.materialistic.data;

import android.content.ContentValues;
import android.content.Intent;
import android.content.ShadowAsyncQueryHandler;
import android.os.Parcel;
import android.support.v4.content.LocalBroadcastManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowLocalBroadcastManager;

import java.util.HashSet;
import java.util.Set;

import io.github.hidroh.materialistic.test.TestWebItem;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.support.v4.Shadows.shadowOf;

@Config(shadows = {ShadowAsyncQueryHandler.class})
@RunWith(RobolectricGradleTestRunner.class)
public class FavoriteManagerTest {
    private ShadowContentResolver resolver;
    private FavoriteManager manager;
    private FavoriteManager.OperationCallbacks callbacks;

    @Before
    public void setUp() {
        callbacks = mock(FavoriteManager.OperationCallbacks.class);
        resolver = shadowOf(ShadowApplication.getInstance().getContentResolver());
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
        manager = new FavoriteManager();
    }

    @Test
    public void testGetNoQuery() {
        manager.get(RuntimeEnvironment.application, null);
        Intent actual = getBroadcastIntent();
        assertThat(actual).hasAction(FavoriteManager.ACTION_GET);
        assertThat((FavoriteManager.Favorite[])
                actual.getParcelableArrayExtra(FavoriteManager.ACTION_GET_EXTRA_DATA)).hasSize(2);
    }

    @Test
    public void testGetEmpty() {
        manager.get(RuntimeEnvironment.application, "blah");
        Intent actual = getBroadcastIntent();
        assertThat(actual).hasAction(FavoriteManager.ACTION_GET);
        assertThat((FavoriteManager.Favorite[])
                actual.getParcelableArrayExtra(FavoriteManager.ACTION_GET_EXTRA_DATA)).isEmpty();
    }

    @Test
    public void testCheckNoId() {
        manager.check(RuntimeEnvironment.application, null, callbacks);
        verify(callbacks, never()).onCheckComplete(anyBoolean());
    }

    @Test
    public void testCheckTrue() {
        manager.check(RuntimeEnvironment.application, "1", callbacks);
        verify(callbacks).onCheckComplete(eq(true));
    }

    @Test
    public void testCheckFalse() {
        manager.check(RuntimeEnvironment.application, "-1", callbacks);
        verify(callbacks).onCheckComplete(eq(false));
    }

    @Test
    public void testAdd() {
        manager.add(RuntimeEnvironment.application, new TestWebItem() {
            @Override
            public String getId() {
                return "3";
            }

            @Override
            public String getUrl() {
                return "http://newitem.com";
            }

            @Override
            public String getDisplayedTitle() {
                return "new title";
            }
        });
        Intent actual = getBroadcastIntent();
        assertThat(actual).hasAction(FavoriteManager.ACTION_ADD);
        assertEquals("3", actual.getStringExtra(FavoriteManager.ACTION_ADD_EXTRA_DATA));
    }

    @Test
    public void testClearAll() {
        manager.clear(RuntimeEnvironment.application, null);
        assertThat(getBroadcastIntent()).hasAction(FavoriteManager.ACTION_CLEAR);
    }

    @Test
    public void testClearQuery() {
        manager.clear(RuntimeEnvironment.application, "blah");
        assertThat(getBroadcastIntent()).hasAction(FavoriteManager.ACTION_CLEAR);
    }

    @Test
    public void testRemoveNoId() {
        manager.remove(RuntimeEnvironment.application, (String) null);
        assertThat(shadowOf(LocalBroadcastManager.getInstance(RuntimeEnvironment.application))
                .getSentBroadcastIntents()).isEmpty();
    }

    @Test
    public void testRemoveId() {
        manager.remove(RuntimeEnvironment.application, "1");
        Intent actual = getBroadcastIntent();
        assertThat(actual).hasAction(FavoriteManager.ACTION_REMOVE);
        assertEquals("1", actual.getStringExtra(FavoriteManager.ACTION_REMOVE_EXTRA_DATA));
    }

    @Test
    public void testRemoveMultipleNoId() {
        manager.remove(RuntimeEnvironment.application, (Set<String>) null);
        assertThat(shadowOf(LocalBroadcastManager.getInstance(RuntimeEnvironment.application))
                .getSentBroadcastIntents()).isEmpty();
    }

    @Test
    public void testRemoveMultiple() {
        manager.remove(RuntimeEnvironment.application, new HashSet<String>(){{add("1");add("2");}});
        assertThat(getBroadcastIntent()).hasAction(FavoriteManager.ACTION_CLEAR);
    }

    @Test
    public void testFavorite() {
        Parcel parcel = Parcel.obtain();
        parcel.writeString("1");
        parcel.writeString("http://example.com");
        parcel.writeString("title");
        parcel.setDataPosition(0);
        FavoriteManager.Favorite favorite = FavoriteManager.Favorite.CREATOR.createFromParcel(parcel);
        assertEquals("title", favorite.getDisplayedTitle());
        assertEquals("example.com", favorite.getSource());
        assertEquals("http://example.com", favorite.getUrl());
        assertEquals("1", favorite.getId());
        assertNotNull(favorite.getDisplayedTime(RuntimeEnvironment.application));
        assertEquals(ItemManager.WebItem.Type.story, favorite.getType());
        assertTrue(favorite.isShareable());
        assertEquals("title - http://example.com", favorite.toString());
        assertEquals(0, favorite.describeContents());
        Parcel output = Parcel.obtain();
        favorite.writeToParcel(output, 0);
        output.setDataPosition(0);
        assertEquals("1", output.readString());
        assertThat(FavoriteManager.Favorite.CREATOR.newArray(1)).hasSize(1);
    }

    private Intent getBroadcastIntent() {
        ShadowLocalBroadcastManager broadcastManager = shadowOf(LocalBroadcastManager.getInstance(RuntimeEnvironment.application));
        return broadcastManager.getSentBroadcastIntents().get(0);
    }
}
