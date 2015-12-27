/*
 * Copyright (c) 2015 Ha Duy Trung
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

import android.view.View;
import android.widget.TextView;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.R;

public class ToggleItemViewHolder extends ItemRecyclerViewAdapter.ItemViewHolder {
    public final TextView mToggle;
    public final View mLevel;
    public final TextView mParent;

    public ToggleItemViewHolder(View itemView) {
        super(itemView);
        mToggle = (TextView) itemView.findViewById(R.id.toggle);
        mLevel = itemView.findViewById(R.id.level);
        mParent = (TextView) itemView.findViewById(R.id.parent);
        AppUtils.setHtmlText(mParent, mParent.getResources().getString(R.string.parent));
    }
}
