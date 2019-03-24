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

package io.github.hidroh.materialistic.test;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TestLayoutManager extends LinearLayoutManager {
    public int firstVisiblePosition = 0;

    public TestLayoutManager(Context context) {
        super(context);
    }

    @Override
    public int findFirstVisibleItemPosition() {
        return firstVisiblePosition;
    }

    @Override
    public void scrollToPositionWithOffset(int position, int offset) {
        firstVisiblePosition = position;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        firstVisiblePosition = position;
    }
}
