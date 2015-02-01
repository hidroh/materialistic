package io.github.hidroh.materialistic.test;

import android.support.v4.widget.SwipeRefreshLayout;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowViewGroup;

@Implements(value = SwipeRefreshLayout.class, inheritImplementationMethods = true)
public class ShadowSwipeRefreshLayout extends ShadowViewGroup {
    private SwipeRefreshLayout.OnRefreshListener mListener;

    @Implementation
    public void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener) {
        mListener = listener;
    }

    public SwipeRefreshLayout.OnRefreshListener getOnRefreshListener() {
        return mListener;
    }
}
