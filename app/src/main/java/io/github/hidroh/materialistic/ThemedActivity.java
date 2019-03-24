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

import android.app.ActivityManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.text.TextUtils;
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
        setTaskTitle(getTitle());
        mMenuTintDelegate.onActivityCreated(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mThemeObservable.subscribe(this, (key, contextChanged) ->  onThemeChanged(key),
                R.string.pref_theme, R.string.pref_daynight_auto);
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

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        setTaskTitle(title);
    }

    protected boolean isDialogTheme() {
        return false;
    }

    protected boolean isTranslucent() {
        return false;
    }

    private void onThemeChanged(int key) {
        if (key == R.string.pref_daynight_auto) {
            AppCompatDelegate.setDefaultNightMode(Preferences.Theme.getAutoDayNightMode(this));
        }
        if (mResumed) {
            AppUtils.restart(this, true);
        } else {
            mPendingThemeChanged = true;
        }
    }

    void setTaskTitle(CharSequence title) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !TextUtils.isEmpty(title)) {
            setTaskDescription(new ActivityManager.TaskDescription(title.toString(),
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_app),
                    ContextCompat.getColor(this, AppUtils.getThemedResId(this, R.attr.colorPrimary))));
        }
    }
}
