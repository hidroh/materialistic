package io.github.hidroh.materialistic;

import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.data.UserManager;
import io.github.hidroh.materialistic.test.ShadowRecyclerView;

import static junit.framework.Assert.assertEquals;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = ShadowRecyclerView.class)
@RunWith(RobolectricGradleTestRunner.class)
public class UserActivityTest {
    private ActivityController<UserActivity> controller;
    private UserActivity activity;
    @Inject UserManager userManager;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Captor ArgumentCaptor<ResponseListener<UserManager.User>> userCaptor;
    @Captor ArgumentCaptor<ResponseListener<ItemManager.Item>> itemCaptor;
    private UserManager.User user;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(userManager);
        reset(itemManager);
        controller = Robolectric.buildActivity(UserActivity.class);
        Intent intent = new Intent();
        intent.putExtra(UserActivity.EXTRA_USERNAME, "username");
        activity = controller.withIntent(intent).create().start().resume().visible().get();
        user = mock(UserManager.User.class);
        when(user.getId()).thenReturn("username");
        when(user.getCreated()).thenReturn(System.currentTimeMillis() / 1000);
        when(user.getKarma()).thenReturn(2016L);
        when(user.getAbout()).thenReturn("about");
        when(user.getItems()).thenReturn(new ItemManager.Item[]{new TestHnItem(1L), new TestHnItem(2L)});
    }

    @Test
    public void testHomeClick() {
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertThat(activity).isFinishing();
    }

    @Test
    public void testBinding() {
        verify(userManager).getUser(eq("username"), userCaptor.capture());
        userCaptor.getValue().onResponse(user);
        assertThat((TextView) activity.findViewById(R.id.title)).hasTextString("username");
        assertThat((TextView) activity.findViewById(R.id.user_info)).containsText("karma: 2016");
        assertThat((TextView) activity.findViewById(R.id.about)).hasTextString("about");
        assertEquals(activity.getString(R.string.submissions_count, 2),
                ((TabLayout) activity.findViewById(R.id.tab_layout)).getTabAt(0).getText());
        assertEquals(2, (((RecyclerView) activity.findViewById(R.id.recycler_view)).getAdapter())
                .getItemCount());
        shadowOf(activity).recreate();
        assertThat((TextView) activity.findViewById(R.id.title)).hasTextString("username");
    }

    @Test
    public void testScrollToTop() {
        verify(userManager).getUser(eq("username"), userCaptor.capture());
        userCaptor.getValue().onResponse(user);
        ShadowRecyclerView recyclerView = (ShadowRecyclerView) ShadowExtractor
                .extract(activity.findViewById(R.id.recycler_view));
        recyclerView.setSmoothScrollToPosition(1);
        TabLayout.Tab tab = ((TabLayout) activity.findViewById(R.id.tab_layout)).getTabAt(0);
        tab.select();
        tab.select();
        assertEquals(0, recyclerView.getSmoothScrollToPosition());
    }

    @Test
    public void testNoId() {
        controller = Robolectric.buildActivity(UserActivity.class).create();
        activity = controller.get();
        assertThat(activity).isFinishing();
    }

    @Test
    public void testNoDataId() {
        controller = Robolectric.buildActivity(UserActivity.class).create();
        Intent intent = new Intent();
        intent.setData(Uri.parse(BuildConfig.APPLICATION_ID + "://user/"));
        activity = controller.withIntent(intent).get();
        assertThat(activity).isFinishing();
    }

    @Test
    public void testItemBinding() {
        verify(userManager).getUser(eq("username"), userCaptor.capture());
        userCaptor.getValue().onResponse(user);
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        RecyclerView.ViewHolder viewHolder = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(viewHolder, 0);
        verify(itemManager).getItem(eq("1"), itemCaptor.capture());
        itemCaptor.getValue().onResponse(new TestHnItem(1L) {
            @Override
            public String getText() {
                return "content";
            }
        });
        adapter.bindViewHolder(viewHolder, 0);
        assertThat((TextView) viewHolder.itemView.findViewById(R.id.text))
                .hasTextString("content");
        viewHolder.itemView.findViewById(R.id.comment).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, ItemActivity.class)
                .hasExtra(ItemActivity.EXTRA_ITEM);
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}
