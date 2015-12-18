package io.github.hidroh.materialistic.test;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowViewGroup;

import java.util.ArrayList;
import java.util.List;

@Implements(value = RecyclerView.class, inheritImplementationMethods = true)
public class ShadowRecyclerView extends ShadowViewGroup {
    private int smoothScrollPosition = -1;
    private List<RecyclerView.ItemDecoration> itemDecorations = new ArrayList<>();
    private ItemTouchHelper.Callback itemTouchHelperCallback;

    @Implementation
    public void smoothScrollToPosition(int position) {
        setSmoothScrollToPosition(position);
    }

    @Implementation
    public void addItemDecoration(RecyclerView.ItemDecoration decor) {
        itemDecorations.add(decor);
    }

    public List<RecyclerView.ItemDecoration> getItemDecorations() {
        return itemDecorations;
    }

    public int getSmoothScrollToPosition() {
        return smoothScrollPosition;
    }

    public void setSmoothScrollToPosition(int position) {
        smoothScrollPosition = position;
    }

    public void setItemTouchHelperCallback(ItemTouchHelper.Callback callback) {
        itemTouchHelperCallback = callback;
    }

    public ItemTouchHelper.Callback getItemTouchHelperCallback() {
        return itemTouchHelperCallback;
    }
}
