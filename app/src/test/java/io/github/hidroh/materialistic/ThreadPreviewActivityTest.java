/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hidroh.materialistic;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;

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

import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.test.ShadowRecyclerView;
import io.github.hidroh.materialistic.test.ShadowRecyclerViewAdapter;

import static junit.framework.Assert.assertEquals;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowRecyclerView.class, ShadowRecyclerViewAdapter.class, ShadowRecyclerViewAdapter.ShadowViewHolder.class})
@RunWith(RobolectricGradleTestRunner.class)
public class ThreadPreviewActivityTest {
    private ActivityController<ThreadPreviewActivity> controller;
    private ThreadPreviewActivity activity;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Inject VolumeNavigationDelegate volumeNavigationDelegate;
    @Captor ArgumentCaptor<ResponseListener<Item>> itemCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(itemManager);
        reset(volumeNavigationDelegate);
        controller = Robolectric.buildActivity(ThreadPreviewActivity.class);
        activity = controller
                .withIntent(new Intent().putExtra(ThreadPreviewActivity.EXTRA_ITEM,
                        new TestHnItem(2L) {
                            @Override
                            public String getBy() {
                                return "username";
                            }
                        }))
                .create().start().resume().visible().get();
    }

    @Test
    public void testNoItem() {
        controller = Robolectric.buildActivity(ThreadPreviewActivity.class);
        activity = controller.create().get();
        assertThat(activity).isFinishing();
    }

    @Test
    public void testHomePressed() {
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertThat(activity).isFinishing();
    }

    @Test
    public void testBinding() {
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        ShadowRecyclerViewAdapter shadowAdapter = (ShadowRecyclerViewAdapter)
                ShadowExtractor.extract(recyclerView.getAdapter());
        shadowAdapter.makeItemVisible(0);
        verify(itemManager).getItem(eq("2"), eq(ItemManager.MODE_DEFAULT), itemCaptor.capture());
        itemCaptor.getValue().onResponse(new TestHnItem(2L) {
            @NonNull
            @Override
            public String getRawType() {
                return Item.COMMENT_TYPE;
            }

            @Override
            public String getText() {
                return "comment";
            }

            @Override
            public String getParent() {
                return "1";
            }

            @Override
            public String getBy() {
                return "username";
            }
        });
        verify(itemManager).getItem(eq("1"), eq(ItemManager.MODE_DEFAULT), itemCaptor.capture());
        itemCaptor.getValue().onResponse(new TestHnItem(1L) {
            @NonNull
            @Override
            public String getRawType() {
                return Item.STORY_TYPE;
            }

            @Override
            public String getTitle() {
                return "story";
            }

            @Override
            public String getBy() {
                return "author";
            }
        });
        RecyclerView.ViewHolder viewHolder1 = shadowAdapter.getViewHolder(0);
        assertThat(viewHolder1.itemView.findViewById(R.id.comment)).isVisible();
        assertEquals(0, recyclerView.getAdapter().getItemViewType(0));
        RecyclerView.ViewHolder viewHolder2 = shadowAdapter.getViewHolder(1);
        assertThat(viewHolder2.itemView.findViewById(R.id.comment)).isNotVisible();
        assertEquals(1, recyclerView.getAdapter().getItemViewType(1));
        viewHolder1.itemView.findViewById(R.id.comment).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, ItemActivity.class)
                .hasExtra(ItemActivity.EXTRA_ITEM);

    }

    @Test
    public void testVolumeNavigation() {
        activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
        verify(volumeNavigationDelegate).setScrollable(any(Scrollable.class), any(AppBarLayout.class));
        verify(volumeNavigationDelegate).onKeyDown(anyInt(), any(KeyEvent.class));
        activity.onKeyUp(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP));
        verify(volumeNavigationDelegate).onKeyUp(anyInt(), any(KeyEvent.class));
        activity.onKeyLongPress(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
        verify(volumeNavigationDelegate).onKeyLongPress(anyInt(), any(KeyEvent.class));
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}
