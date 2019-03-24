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
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.CustomShadows;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerViewAdapter;

import static junit.framework.Assert.assertEquals;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowRecyclerViewAdapter.class})
@RunWith(TestRunner.class)
public class ThreadPreviewActivityTest {
    private ActivityController<ThreadPreviewActivity> controller;
    private ThreadPreviewActivity activity;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Inject KeyDelegate keyDelegate;
    @Captor ArgumentCaptor<ResponseListener<Item>> itemCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(itemManager);
        reset(keyDelegate);
        controller = Robolectric.buildActivity(ThreadPreviewActivity.class,
                new Intent().putExtra(ThreadPreviewActivity.EXTRA_ITEM,
                        new TestHnItem(2L) {
                            @Override
                            public String getBy() {
                                return "username";
                            }
                        }));
        activity = controller
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
        RecyclerView recyclerView = activity.findViewById(R.id.recycler_view);
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
        RecyclerView.ViewHolder viewHolder1 = CustomShadows.customShadowOf(recyclerView.getAdapter())
                .getViewHolder(0);
        recyclerView.getAdapter().bindViewHolder(viewHolder1, 0); // TODO should not need this
        assertThat((View) viewHolder1.itemView.findViewById(R.id.comment)).isVisible();
        assertEquals(0, recyclerView.getAdapter().getItemViewType(0));
        RecyclerView.ViewHolder viewHolder2 = CustomShadows.customShadowOf(recyclerView.getAdapter())
                .getViewHolder(1);
        assertThat((View) viewHolder2.itemView.findViewById(R.id.comment)).isNotVisible();
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
