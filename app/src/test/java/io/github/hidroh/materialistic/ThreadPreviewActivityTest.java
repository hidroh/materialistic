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
import android.support.v7.widget.RecyclerView;

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

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.TestHnItem;

import static junit.framework.Assert.assertEquals;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
public class ThreadPreviewActivityTest {
    private ActivityController<ThreadPreviewActivity> controller;
    private ThreadPreviewActivity activity;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Captor ArgumentCaptor<ResponseListener<ItemManager.Item>> itemCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(itemManager);
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
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        RecyclerView.ViewHolder viewHolder2 = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(viewHolder2, 0);
        verify(itemManager).getItem(eq("2"), itemCaptor.capture());
        itemCaptor.getValue().onResponse(new TestHnItem(2L) {
            @NonNull
            @Override
            public String getRawType() {
                return ItemManager.Item.COMMENT_TYPE;
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
        adapter.bindViewHolder(viewHolder2, 0);
        RecyclerView.ViewHolder viewHolder1 = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(viewHolder1, 0);
        verify(itemManager, times(2)).getItem(eq("1"), itemCaptor.capture());
        itemCaptor.getValue().onResponse(new TestHnItem(1L) {
            @NonNull
            @Override
            public String getRawType() {
                return ItemManager.Item.STORY_TYPE;
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
        adapter.bindViewHolder(viewHolder1, 0);
        assertThat(viewHolder1.itemView.findViewById(R.id.comment)).isVisible();
        assertEquals(0, adapter.getItemViewType(0));
        viewHolder2 = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(viewHolder2, 1);
        assertThat(viewHolder2.itemView.findViewById(R.id.comment)).isNotVisible();
        assertEquals(1, adapter.getItemViewType(1));
        viewHolder1.itemView.findViewById(R.id.comment).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, ItemActivity.class)
                .hasExtra(ItemActivity.EXTRA_ITEM);

    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}
