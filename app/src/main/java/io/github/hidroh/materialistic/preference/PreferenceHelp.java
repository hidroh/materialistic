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

package io.github.hidroh.materialistic.preference;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;

import io.github.hidroh.materialistic.R;

public class PreferenceHelp extends PreferenceGroup {
    private final int mLayoutResId;
    private final String mTitle;

    public PreferenceHelp(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.preferenceHelpStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceHelp);
        try {
            mLayoutResId = a.getResourceId(R.styleable.PreferenceHelp_dialogLayout, 0);
            mTitle = a.getString(R.styleable.PreferenceHelp_dialogTitle);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onClick() {
        if (mLayoutResId == 0) {
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle(mTitle)
                .setView(mLayoutResId)
                .setPositiveButton(R.string.got_it, null)
                .create()
                .show();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
    }
}
