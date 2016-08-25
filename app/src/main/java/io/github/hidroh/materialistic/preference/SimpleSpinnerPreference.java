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

package io.github.hidroh.materialistic.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.github.hidroh.materialistic.R;

public class SimpleSpinnerPreference extends SpinnerPreference {

    private final LayoutInflater mLayoutInflater;

    @SuppressWarnings("unused")
    public SimpleSpinnerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleSpinnerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
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
        textView.setText(mEntries[position]);
    }
}
