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

import androidx.collection.ArrayMap;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.util.ReflectionHelpers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.robolectric.shadow.api.Shadow.directlyOn;

@SuppressWarnings("unchecked")
@Implements(RecyclerView.Adapter.class)
public class ShadowRecyclerViewAdapter {
    private final Map<Integer, RecyclerView.ViewHolder> holders = new ArrayMap<>();
    @RealObject RecyclerView.Adapter realObject;
    private RecyclerView recyclerView;
    private Set<Integer> notified = new HashSet<>();

    @Implementation
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        directly().onAttachedToRecyclerView(recyclerView);
    }

    @Implementation
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        recyclerView.getLayoutManager().removeAllViews();
        this.recyclerView = null;
        directly().onDetachedFromRecyclerView(recyclerView);
    }

    @Implementation
    public final RecyclerView.ViewHolder createViewHolder(ViewGroup parent, int viewType) {
        exitLayout();
        return directly().createViewHolder(parent, viewType);
    }

    @Implementation
    public void bindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holders.containsKey(position) && !notified.contains(position)) {
            return;
        }
        exitLayout();
        notified.remove(position);
        holders.put(position, holder);
        directly().bindViewHolder(holder, position);
    }

    @Implementation
    public void notifyDataSetChanged() {
        if (recyclerView != null) {
            prepare(0, realObject.getItemCount() - 1);
        }
        directly().notifyDataSetChanged();
        relayout();
    }

    @Implementation
    public void notifyItemChanged(int position) {
        prepare(position, position);
        directly().notifyItemChanged(position);
        relayout();
    }

    @Implementation
    public void notifyItemChanged(int position, Object payload) {
        prepare(position, position);
        directly().notifyItemChanged(position, payload);
        relayout();
    }

    @Implementation
    public void notifyItemRangeChanged(int positionStart, int itemCount) {
        prepare(positionStart, positionStart+itemCount-1);
        directly().notifyItemRangeChanged(positionStart, itemCount);
        relayout();
    }

    @Implementation
    public void notifyItemRangeChanged(int positionStart, int itemCount, Object payload) {
        prepare(positionStart, positionStart+itemCount-1);
        directly().notifyItemRangeChanged(positionStart, itemCount, payload);
        relayout();
    }

    @Implementation
    public void notifyItemInserted(int position) {
        prepare(position, realObject.getItemCount()-1);
        directly().notifyItemInserted(position);
        relayout();
    }

    @Implementation
    public void notifyItemMoved(int fromPosition, int toPosition) {
        prepare(fromPosition, fromPosition);
        prepare(toPosition, toPosition);
        directly().notifyItemMoved(fromPosition, toPosition);
        relayout();
    }

    @Implementation
    public void notifyItemRangeInserted(int positionStart, int itemCount) {
        prepare(positionStart, realObject.getItemCount()-1);
        directly().notifyItemRangeInserted(positionStart, itemCount);
        relayout();
    }

    @Implementation
    public void notifyItemRemoved(int position) {
        prepare(position, realObject.getItemCount()-1);
        directly().notifyItemRemoved(position);
        relayout();
    }

    @Implementation
    public void notifyItemRangeRemoved(int positionStart, int itemCount) {
        prepare(positionStart, realObject.getItemCount()-1);
        directly().notifyItemRangeRemoved(positionStart, itemCount);
        relayout();
    }

    public RecyclerView.ViewHolder getViewHolder(int position) {
        return holders.get(position);
    }

    private void prepare(int start, int end) {
        for (int i = start; i <= end; i++) {
            notified.add(i);
        }
    }

    private void exitLayout() {
        if (recyclerView != null && recyclerView.isComputingLayout()) {
            ReflectionHelpers.callInstanceMethod(recyclerView, "onExitLayoutOrScroll");
        }
    }

    private void relayout() {
        if (recyclerView != null) {
            recyclerView.measure(0, 0);
            recyclerView.layout(0, 0, 100, 1000);
        }
    }

    private RecyclerView.Adapter directly() {
        return directlyOn(realObject, RecyclerView.Adapter.class);
    }
}
