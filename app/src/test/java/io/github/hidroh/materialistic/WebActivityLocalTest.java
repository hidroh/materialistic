package io.github.hidroh.materialistic;

import android.content.Intent;
import android.support.annotation.NonNull;
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
import org.robolectric.util.ActivityController;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.test.TestItem;
import io.github.hidroh.materialistic.test.TestWebItem;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
public class WebActivityLocalTest {
    private ActivityController<WebActivity> controller;
    private WebActivity activity;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Captor ArgumentCaptor<ItemManager.ResponseListener<ItemManager.Item>> listener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(itemManager);
        controller = Robolectric.buildActivity(WebActivity.class);
        activity = controller.get();
    }

    @Test
    public void testStory() {
        TestWebItem item = new TestWebItem() {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }

            @Override
            public String getId() {
                return "1";
            }

            @Override
            public String getUrl() {
                return String.format(HackerNewsClient.WEB_ITEM_PATH, "1");
            }

            @Override
            public String getDisplayedTitle() {
                return "Ask HN";
            }
        };
        Intent intent = new Intent();
        intent.putExtra(WebActivity.EXTRA_ITEM, item);
        controller.withIntent(intent).create().start().resume().visible();
        assertThat((TextView) activity.findViewById(R.id.title)).hasTextString("Ask HN");
        verify(itemManager).getItem(eq("1"), listener.capture());
        listener.getValue().onResponse(new TestItem() {
            @Override
            public String getText() {
                return "text";
            }
        });
        assertThat((TextView) activity.findViewById(R.id.text)).hasTextString("text");
    }

    @Test
    public void testComment() {
        TestItem item = new TestItem() {
            @NonNull
            @Override
            public String getType() {
                return COMMENT_TYPE;
            }

            @Override
            public String getId() {
                return "1";
            }

            @Override
            public String getUrl() {
                return String.format(HackerNewsClient.WEB_ITEM_PATH, "1");
            }

            @Override
            public String getText() {
                return "comment";
            }
        };
        Intent intent = new Intent();
        intent.putExtra(WebActivity.EXTRA_ITEM, item);
        controller.withIntent(intent).create().start().resume().visible();
        assertThat(activity.findViewById(R.id.title)).isNotVisible();
        assertThat((TextView) activity.findViewById(R.id.text)).hasTextString("comment");
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}
