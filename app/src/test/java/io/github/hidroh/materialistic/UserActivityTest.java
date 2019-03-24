package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.google.android.material.tabs.TabLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.android.controller.ActivityController;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.data.UserManager;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerView;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerViewAdapter;

import static io.github.hidroh.materialistic.test.shadow.CustomShadows.customShadowOf;
import static junit.framework.Assert.assertEquals;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = ShadowRecyclerViewAdapter.class)
@RunWith(TestRunner.class)
public class UserActivityTest {
    private ActivityController<UserActivity> controller;
    private UserActivity activity;
    @Inject UserManager userManager;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Inject KeyDelegate keyDelegate;
    @Captor ArgumentCaptor<ResponseListener<UserManager.User>> userCaptor;
    @Captor ArgumentCaptor<ResponseListener<Item>> itemCaptor;
    private UserManager.User user;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(userManager);
        reset(itemManager);
        reset(keyDelegate);
        Intent intent = new Intent();
        intent.putExtra(UserActivity.EXTRA_USERNAME, "username");
        controller = Robolectric.buildActivity(UserActivity.class, intent);
        activity = controller.create().start().resume().visible().get();
        user = mock(UserManager.User.class);
        when(user.getId()).thenReturn("username");
        when(user.getCreated(any(Context.class))).thenReturn("May 01 2015");
        when(user.getKarma()).thenReturn(2016L);
        when(user.getAbout()).thenReturn("about");
        when(user.getItems()).thenReturn(new Item[]{
                new TestHnItem(1L){
                    @NonNull
                    @Override
                    public String getType() {
                        return COMMENT_TYPE;
                    }
                },
                new TestHnItem(2L) {
                    @NonNull
                    @Override
                    public String getType() {
                        return STORY_TYPE;
                    }
                }
        });
    }

    @Test
    public void testBinding() {
        verify(userManager).getUser(eq("username"), userCaptor.capture());
        userCaptor.getValue().onResponse(user);
        assertThat((TextView) activity.findViewById(R.id.title)).hasTextString("username (2,016)");
        assertThat((TextView) activity.findViewById(R.id.about)).hasTextString("about");
        assertEquals(activity.getResources().getQuantityString(R.plurals.submissions_count, 2, 2),
                ((TabLayout) activity.findViewById(R.id.tab_layout)).getTabAt(0).getText());
        assertEquals(2, (((RecyclerView) activity.findViewById(R.id.recycler_view)).getAdapter())
                .getItemCount());
        activity.recreate();
        assertThat((TextView) activity.findViewById(R.id.title)).hasTextString("username (2,016)");
    }

    @Test
    public void testBindingNoAbout() {
        when(user.getAbout()).thenReturn(null);
        verify(userManager).getUser(eq("username"), userCaptor.capture());
        userCaptor.getValue().onResponse(user);
        assertThat((TextView) activity.findViewById(R.id.about)).isNotVisible();
    }

    @Test
    public void testEmpty() {
        verify(userManager).getUser(eq("username"), userCaptor.capture());
        userCaptor.getValue().onResponse(null);
        assertThat((View) activity.findViewById(R.id.empty)).isVisible();
    }

    @Test
    public void testFailed() {
        verify(userManager).getUser(eq("username"), userCaptor.capture());
        userCaptor.getValue().onError(null);
        assertEquals(activity.getString(R.string.user_failed), ShadowToast.getTextOfLatestToast());
    }

    @Config(shadows = ShadowRecyclerView.class)
    @Test
    public void testScrollToTop() {
        verify(userManager).getUser(eq("username"), userCaptor.capture());
        userCaptor.getValue().onResponse(user);
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        recyclerView.smoothScrollToPosition(1);
        assertThat(customShadowOf(recyclerView).getScrollPosition()).isEqualTo(1);
        TabLayout.Tab tab = ((TabLayout) activity.findViewById(R.id.tab_layout)).getTabAt(0);
        tab.select();
        tab.select();
        assertThat(customShadowOf(recyclerView).getScrollPosition()).isEqualTo(0);
    }

    @Test
    public void testNoId() {
        controller = Robolectric.buildActivity(UserActivity.class);
        activity = controller.create().get();
        assertThat(activity).isFinishing();
    }

    @Test
    public void testNoDataId() {
        Intent intent = new Intent();
        intent.setData(Uri.parse(BuildConfig.APPLICATION_ID + "://user/"));
        controller = Robolectric.buildActivity(UserActivity.class, intent);
        activity = controller.create().get();
        assertThat(activity).isFinishing();
    }

    @Test
    public void testWithDataId() {
        Intent intent = new Intent();
        intent.setData(Uri.parse(BuildConfig.APPLICATION_ID + "://user/123"));
        controller = Robolectric.buildActivity(UserActivity.class, intent);
        activity = controller.create().get();
        assertThat(activity).isNotFinishing();
    }

    @Test
    public void testDeepLink() {
        Intent intent = new Intent();
        intent.setData(Uri.parse("https://news.ycombinator.com/user?id=123"));
        controller = Robolectric.buildActivity(UserActivity.class, intent);
        activity = controller.create().get();
        assertThat(activity).isNotFinishing();
    }

    @Test
    public void testCommentBinding() {
        verify(userManager).getUser(eq("username"), userCaptor.capture());
        userCaptor.getValue().onResponse(user);
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        verify(itemManager).getItem(eq("1"),
                eq(ItemManager.MODE_DEFAULT),
                itemCaptor.capture());
        itemCaptor.getValue().onResponse(new TestHnItem(1L) {
            @Override
            public String getText() {
                return "content";
            }

            @Override
            public String getParent() {
                return "2";
            }
        });
        RecyclerView.ViewHolder viewHolder = customShadowOf(recyclerView.getAdapter()).getViewHolder(0);
        assertThat((View) viewHolder.itemView.findViewById(R.id.title)).isNotVisible();
        assertThat((TextView) viewHolder.itemView.findViewById(R.id.text))
                .isVisible()
                .hasTextString("content");
        viewHolder.itemView.findViewById(R.id.comment).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, ThreadPreviewActivity.class)
                .hasExtra(ThreadPreviewActivity.EXTRA_ITEM);
    }

    @Test
    public void testStoryBinding() {
        verify(userManager).getUser(eq("username"), userCaptor.capture());
        userCaptor.getValue().onResponse(user);
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        verify(itemManager).getItem(eq("2"),
                eq(ItemManager.MODE_DEFAULT),
                itemCaptor.capture());
        itemCaptor.getValue().onResponse(new TestHnItem(2L) {
            @Override
            public String getTitle() {
                return "title";
            }

            @Override
            public String getText() {
                return "content";
            }

            @Override
            public int getScore() {
                return 46;
            }
        });
        RecyclerView.ViewHolder viewHolder = customShadowOf(recyclerView.getAdapter()).getViewHolder(1);
        assertThat((TextView) viewHolder.itemView.findViewById(R.id.posted))
                .containsText(activity.getResources().getQuantityString(R.plurals.score, 46, 46));
        assertThat((TextView) viewHolder.itemView.findViewById(R.id.title))
                .isVisible()
                .hasTextString("title");
        assertThat((TextView) viewHolder.itemView.findViewById(R.id.text))
                .isVisible()
                .hasTextString("content");
        viewHolder.itemView.findViewById(R.id.comment).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, ItemActivity.class)
                .hasExtra(ItemActivity.EXTRA_ITEM);
    }

    @Test
    public void testDeletedItemBinding() {
        verify(userManager).getUser(eq("username"), userCaptor.capture());
        userCaptor.getValue().onResponse(user);
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        verify(itemManager).getItem(eq("1"),
                eq(ItemManager.MODE_DEFAULT),
                itemCaptor.capture());
        itemCaptor.getValue().onResponse(new TestHnItem(1L) {
            @Override
            public boolean isDeleted() {
                return true;
            }
        });
        RecyclerView.ViewHolder viewHolder = customShadowOf(recyclerView.getAdapter()).getViewHolder(0);
        assertThat((View) viewHolder.itemView.findViewById(R.id.comment)).isNotVisible();
    }

    @Test
    public void testVolumeNavigation() {
        activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
        verify(keyDelegate).setScrollable(any(Scrollable.class), any());
        verify(keyDelegate).onKeyDown(anyInt(), any(KeyEvent.class));
        activity.onKeyUp(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP));
        verify(keyDelegate).onKeyUp(anyInt(), any(KeyEvent.class));
        activity.onKeyLongPress(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
        verify(keyDelegate).onKeyLongPress(anyInt(), any(KeyEvent.class));
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}
