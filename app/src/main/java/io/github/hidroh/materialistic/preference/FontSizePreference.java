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
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;

public class FontSizePreference extends SpinnerPreference {
    private final LayoutInflater mLayoutInflater;

    @SuppressWarnings("unused")
    public FontSizePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FontSizePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLayoutInflater = LayoutInflater.from(getContext());
    }

    @Override
    protected View createDropDownView(int position, ViewGroup parent) {
        return mLayoutInflater.inflate(R.layout.support_simple_spinner_dropdown_item, parent, false);
    }

    @Override
    protected void bindDropDownView(int position, View view) {
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        float textSize = AppUtils.getDimension(getContext(),
                Preferences.Theme.resolveTextSize(mEntryValues[position]),
                R.attr.contentTextSize);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        textView.setText(mEntries[position]);
    }
}
