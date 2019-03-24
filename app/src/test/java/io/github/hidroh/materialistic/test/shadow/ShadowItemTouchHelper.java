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

import static io.github.hidroh.materialistic.test.shadow.CustomShadows.customShadowOf;

@Implements(ItemTouchHelper.class)
public class ShadowItemTouchHelper {
    private ItemTouchHelper.Callback callback;

    public void __constructor__(ItemTouchHelper.Callback callback) {
        this.callback = callback;
    }

    @Implementation
    public void attachToRecyclerView(RecyclerView recyclerView) {
        if (recyclerView == null) {
            return;
        }
        customShadowOf(recyclerView).setItemTouchHelperCallback(this.callback);
    }
}
