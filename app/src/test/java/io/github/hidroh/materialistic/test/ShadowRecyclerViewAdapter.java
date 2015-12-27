package io.github.hidroh.materialistic.test;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.internal.ShadowExtractor;

@Implements(value = RecyclerView.Adapter.class, inheritImplementationMethods = true)
public class ShadowRecyclerViewAdapter {
    private RecyclerView recyclerView;
    @RealObject
    RecyclerView.Adapter realObject;
    private final SparseArray<RecyclerView.ViewHolder> holders = new SparseArray<>();

    @Implementation
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Implementation
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        realObject.onBindViewHolder(holder, position);
        holders.put(position, holder);
    }

    @Implementation
    public void notifyItemChanged(int position) {
        makeItemVisible(position);
    }

    @Implementation
    public void notifyItemInserted(int position) {
        makeItemVisible(position);
    }

    @Implementation
    public void notifyDataSetChanged() {
        for (int i = 0; i < holders.size(); i++) {
            notifyItemChanged(holders.keyAt(i));
        }
    }

    @Implementation
    public void notifyItemRangeChanged(int positionStart, int itemCount) {
        for (int i = positionStart; i < itemCount; i++) {
            notifyItemChanged(i);
        }
    }

    public void makeItemVisible(int position) {
        RecyclerView.ViewHolder holder = realObject
                .createViewHolder(recyclerView, realObject.getItemViewType(position));
        ((ShadowViewHolder) ShadowExtractor.extract(holder)).setAdapterPosition(position);
        onBindViewHolder(holder, position);
    }

    public RecyclerView.ViewHolder getViewHolder(int position) {
        return holders.get(position);
    }

    @Implements(RecyclerView.ViewHolder.class)
    public static class ShadowViewHolder {
        private int adapterPosition;

        public int getAdapterPosition() {
            return adapterPosition;
        }

        public void setAdapterPosition(int adapterPosition) {
            this.adapterPosition = adapterPosition;
        }
    }
}
