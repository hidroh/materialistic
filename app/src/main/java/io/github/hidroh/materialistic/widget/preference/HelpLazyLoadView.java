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

package io.github.hidroh.materialistic.widget.preference;

import android.content.Context;
import com.google.android.material.tabs.TabLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ScrollView;

import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;

public class HelpLazyLoadView extends ScrollView {
    public HelpLazyLoadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        addView(LayoutInflater.from(context).inflate(R.layout.include_help_lazy_load, this, false));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.comments));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.article));
        Preferences.StoryViewMode defaultView = Preferences.getDefaultStoryView(getContext());
        int defaultTab;
        switch (defaultView) {
            case Comment:
            default:
                defaultTab = 0;
                break;
            case Article:
            case Readability:
                defaultTab = 1;
                break;
        }
        //noinspection ConstantConditions
        tabLayout.getTabAt(defaultTab).select();
    }
}
