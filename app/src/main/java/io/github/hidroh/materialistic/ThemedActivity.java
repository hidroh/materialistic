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

package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;

public abstract class ThemedActivity extends AppCompatActivity {
    private final MenuTintDelegate mMenuTintDelegate = new MenuTintDelegate();
    private final Preferences.Observable mThemeObservable = new Preferences.Observable();
    private boolean mResumed = true;
    private boolean mPendingThemeChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Preferences.Theme.apply(this, isDialogTheme(), isTranslucent());
        super.onCreate(savedInstanceState);
        mMenuTintDelegate.onActivityCreated(this);
        mThemeObservable.subscribe(this, (key, contextChanged) ->  onThemeChanged(),
                R.string.pref_theme,
                R.string.pref_font,
                R.string.pref_text_size);
    }

    @CallSuper
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenuTintDelegate.onOptionsMenuCreated(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResumed = true;
        if (mPendingThemeChanged) {
            AppUtils.restart(this, false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mThemeObservable.unsubscribe(this);
    }

    protected boolean isDialogTheme() {
        return false;
    }

    protected boolean isTranslucent() {
        return false;
    }

    private void onThemeChanged() {
        if (mResumed) {
            AppUtils.restart(this, true);
        } else {
            mPendingThemeChanged = true;
        }
    }
}
