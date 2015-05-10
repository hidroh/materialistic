package io.github.hidroh.materialistic.test;

import android.support.v7.widget.RecyclerView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowViewGroup;

@Implements(value = RecyclerView.class, inheritImplementationMethods = true)
public class ShadowRecyclerView extends ShadowViewGroup {
    private int smoothScrollPosition = -1;

    @Implementation
    public void smoothScrollToPosition(int position) {
        setSmoothScrollToPosition(position);
    }

    public int getSmoothScrollToPosition() {
        return smoothScrollPosition;
    }

    public void setSmoothScrollToPosition(int position) {
        smoothScrollPosition = position;
    }
}
