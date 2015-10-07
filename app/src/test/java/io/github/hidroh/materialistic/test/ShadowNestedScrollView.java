package io.github.hidroh.materialistic.test;

import android.support.v4.widget.NestedScrollView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowViewGroup;

@Implements(value = NestedScrollView.class, inheritImplementationMethods = true)
public class ShadowNestedScrollView extends ShadowViewGroup {
    private int smoothScrollY = -1;

    @Implementation
    public  void smoothScrollTo(int x, int y) {
        setSmoothScrollY(y);
    }

    public int getSmoothScrollY() {
        return smoothScrollY;
    }

    public void setSmoothScrollY(int position) {
        smoothScrollY = position;
    }
}
