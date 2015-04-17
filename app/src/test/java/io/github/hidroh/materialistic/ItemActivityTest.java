package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowResolveInfo;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.test.TestItem;

import static junit.framework.Assert.assertEquals;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class ItemActivityTest {
    private ActivityController<ItemActivity> controller;
    private ItemActivity activity;
    @Inject @Named(ActivityModule.HN) ItemManager hackerNewsClient;
    @Inject FavoriteManager favoriteManager;
    @Captor ArgumentCaptor<ItemManager.ResponseListener<ItemManager.Item>> listener;
    @Captor ArgumentCaptor<FavoriteManager.OperationCallbacks> callbacks;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(hackerNewsClient);
        reset(favoriteManager);
        controller = Robolectric.buildActivity(ItemActivity.class);
        activity = controller.get();
    }

    @Test
    public void testStoryGivenItemId() {
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM_ID, "1");
        intent.putExtra(ItemActivity.EXTRA_ITEM_LEVEL, 2);
        controller.withIntent(intent).create().start().resume();
        verify(hackerNewsClient).getItem(eq("1"), listener.capture());
        listener.getValue().onResponse(new TestItem() {
            @Override
            public Type getType() {
                return Type.story;
            }

            @Override
            public int getKidCount() {
                return 1;
            }

            @Override
            public String getDisplayedTitle() {
                return "title";
            }

            @Override
            public String getSource() {
                return "http://example.com";
            }

            @Override
            public boolean isShareable() {
                return true;
            }
        });
        assertThat(activity).hasTitle(activity.getString(R.string.title_activity_item_count, 1));
        assertThat((TextView) activity.findViewById(R.id.source)).hasTextString("http://example.com");
        TextView titleTextView = (TextView) activity.findViewById(android.R.id.text2);
        assertThat(titleTextView).hasTextString("title")
                .hasEllipsize(TextUtils.TruncateAt.END);
        activity.findViewById(R.id.header_card_view).performClick();
        assertEquals(WebActivity.class.getName(),
                shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
    }

    @Test
    public void testStoryGivenWebItem() {
        Intent intent = new Intent();
        ItemManager.WebItem webItem = mock(ItemManager.WebItem.class);
        when(webItem.getId()).thenReturn("1");
        intent.putExtra(ItemActivity.EXTRA_ITEM, webItem);
        controller.withIntent(intent).create().start().resume();
        verify(hackerNewsClient).getItem(eq("1"), any(ItemManager.ResponseListener.class));
    }

    @Test
    public void testJobGivenDeepLink() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(ItemActivity.EXTRA_ITEM_LEVEL, 1);
        intent.setData(Uri.parse("https://news.ycombinator.com/item?id=1"));
        controller.withIntent(intent).create().start().resume();
        verify(hackerNewsClient).getItem(eq("1"), listener.capture());
        listener.getValue().onResponse(new TestItem() {
            @Override
            public Type getType() {
                return Type.job;
            }

            @Override
            public boolean isShareable() {
                return true;
            }
        });
        assertThat(activity.findViewById(R.id.source)).isNotVisible();
        assertEquals(R.drawable.ic_work_grey600_18dp,
                shadowOf(((TextView) activity.findViewById(R.id.posted))
                        .getCompoundDrawables()[0]).getCreatedFromResId());
    }

    @Test
    public void testPoll() {
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM_LEVEL, 3);
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestItem() {
            @Override
            public Type getType() {
                return Type.poll;
            }

            @Override
            public boolean isShareable() {
                return true;
            }
        });
        controller.withIntent(intent).create().start().resume();
        assertThat(activity.findViewById(R.id.source)).isNotVisible();
        assertEquals(R.drawable.ic_poll_grey600_18dp,
                shadowOf(((TextView) activity.findViewById(R.id.posted))
                        .getCompoundDrawables()[0]).getCreatedFromResId());
    }

    @Test
    public void testOptionExternal() {
        RobolectricPackageManager packageManager = (RobolectricPackageManager)
                RuntimeEnvironment.application.getPackageManager();
        packageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com")),
                ShadowResolveInfo.newResolveInfo("label", activity.getPackageName(),
                        WebActivity.class.getName()));
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestItem() {
            @Override
            public Type getType() {
                return Type.comment;
            }

            @Override
            public String getUrl() {
                return "http://example.com";
            }
        });
        controller.withIntent(intent).create().start().resume();
        activity.onOptionsItemSelected(new RoboMenuItem() {
            @Override
            public int getItemId() {
                return R.id.menu_external;
            }
        });
        Intent actual = shadowOf(activity).getNextStartedActivity();
        assertThat(actual).hasAction(Intent.ACTION_VIEW);
    }

    @Test
    public void testFavoriteStory() {
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestItem() {
            @Override
            public Type getType() {
                return Type.story;
            }

            @Override
            public boolean isShareable() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        controller.withIntent(intent).create().start().resume();
        verify(favoriteManager).check(any(Context.class), eq("1"), callbacks.capture());
        callbacks.getValue().onCheckComplete(true);
        assertThat(activity.findViewById(R.id.bookmarked)).isVisible();
        activity.findViewById(R.id.header_card_view).performLongClick();
        assertThat(activity.findViewById(R.id.bookmarked)).isNotVisible();
    }

    @Test
    public void testNonFavoriteStory() {
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestItem() {
            @Override
            public Type getType() {
                return Type.story;
            }

            @Override
            public boolean isShareable() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        controller.withIntent(intent).create().start().resume();
        verify(favoriteManager).check(any(Context.class), eq("1"), callbacks.capture());
        callbacks.getValue().onCheckComplete(false);
        assertThat(activity.findViewById(R.id.bookmarked)).isNotVisible();
        activity.findViewById(R.id.header_card_view).performLongClick();
        assertThat(activity.findViewById(R.id.bookmarked)).isVisible();
    }

    @After
    public void tearDown() {
        reset(hackerNewsClient);
        reset(favoriteManager);
        controller.pause().stop().destroy();
    }
}
