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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public abstract class DrawerActivity extends InjectableActivity {

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private View mDrawer;
    private Class<? extends Activity> mPendingNavigation;
    private Bundle mPendingNavigationExtras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_drawer);
        mDrawer = findViewById(R.id.drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open_drawer,
                R.string.close_drawer) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (drawerView.equals(mDrawer) && mPendingNavigation != null) {
                    final Intent intent = new Intent(DrawerActivity.this, mPendingNavigation);
                    if (mPendingNavigationExtras != null) {
                        intent.putExtras(mPendingNavigationExtras);
                        mPendingNavigationExtras = null;
                    }
                    // TODO M bug https://code.google.com/p/android/issues/detail?id=193822
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    mPendingNavigation = null;
                }
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item)|| super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mDrawer)) {
            closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDrawerLayout.removeDrawerListener(mDrawerToggle);
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup drawerLayout = (ViewGroup) findViewById(R.id.drawer_layout);
        View view = getLayoutInflater().inflate(layoutResID, drawerLayout, false);
        //noinspection ConstantConditions
        drawerLayout.addView(view, 0);
    }

    void navigate(Class<? extends Activity> activityClass, @Nullable Bundle extras) {
        mPendingNavigation = !getClass().equals(activityClass) ? activityClass : null;
        mPendingNavigationExtras = extras;
        closeDrawers();
    }

    private void closeDrawers() {
        mDrawerLayout.closeDrawers();
    }
}
