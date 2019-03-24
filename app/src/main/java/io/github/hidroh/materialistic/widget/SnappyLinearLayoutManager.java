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

package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.graphics.PointF;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import io.github.hidroh.materialistic.AppUtils;

/**
 * Light extension to {@link LinearLayoutManager} that overrides smooth scroller to
 * always snap to start
 */
public class SnappyLinearLayoutManager extends LinearLayoutManager {

    private final int mExtraSpace;

    public SnappyLinearLayoutManager(Context context, boolean preload) {
        super(context);
        mExtraSpace = preload ? AppUtils.getDisplayHeight(context) : 0;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView,
                                       RecyclerView.State state,
                                       int position) {
        RecyclerView.SmoothScroller smoothScroller =
                new LinearSmoothScroller(recyclerView.getContext()) {
                    @Override
                    public PointF computeScrollVectorForPosition(int targetPosition) {
                        return SnappyLinearLayoutManager.this
                                .computeScrollVectorForPosition(targetPosition);
                    }

                    @Override
                    protected int getVerticalSnapPreference() {
                        return SNAP_TO_START; // override base class behavior
                    }
                };
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    @Override
    protected int getExtraLayoutSpace(RecyclerView.State state) {
        return mExtraSpace == 0 ? super.getExtraLayoutSpace(state) : mExtraSpace;
    }
}

