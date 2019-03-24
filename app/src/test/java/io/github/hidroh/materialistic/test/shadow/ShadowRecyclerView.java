/*
 * Copyright (c) 2016 Ha Duy Trung
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

package io.github.hidroh.materialistic.test.shadow;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowViewGroup;

import static org.robolectric.shadow.api.Shadow.directlyOn;

@Implements(RecyclerView.class)
public class ShadowRecyclerView extends ShadowViewGroup {
    @RealObject RecyclerView realObject;
    private int scrollPosition;
    private ItemTouchHelper.Callback itemTouchHelperCallback;
    private RecyclerView.OnScrollListener scrollListener;

    @Implementation
    public void smoothScrollToPosition(int position) {
        directly().smoothScrollToPosition(position);
        scrollPosition = position;
    }

    @Implementation
    public void scrollToPosition(int position) {
        directly().scrollToPosition(position);
        scrollPosition = position;
    }

    @Implementation
    public void addOnScrollListener(RecyclerView.OnScrollListener scrollListener) {
        this.scrollListener = scrollListener;
    }

    public void removeOnScrollListener(RecyclerView.OnScrollListener listener) {
        if (scrollListener == listener) {
            scrollListener = null;
        }
    }

    public ItemTouchHelper.Callback getItemTouchHelperCallback() {
        return itemTouchHelperCallback;
    }

    public int getScrollPosition() {
        return scrollPosition;
    }

    public RecyclerView.OnScrollListener getScrollListener() {
        return scrollListener;
    }

    void setItemTouchHelperCallback(ItemTouchHelper.Callback callback) {
        itemTouchHelperCallback = callback;
    }

    private RecyclerView directly() {
        return directlyOn(realObject, RecyclerView.class);
    }
}
