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
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.Window;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.widget.CommentItemDecoration;
import io.github.hidroh.materialistic.widget.SnappyLinearLayoutManager;
import io.github.hidroh.materialistic.widget.ThreadPreviewRecyclerViewAdapter;

public class ThreadPreviewActivity extends InjectableActivity {
    public static final String EXTRA_ITEM = ThreadPreviewActivity.class.getName() + ".EXTRA_ITEM";

    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    @Inject VolumeNavigationDelegate mVolumeNavigationDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        Item item = getIntent().getParcelableExtra(EXTRA_ITEM);
        if (item == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_thread_preview);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new SnappyLinearLayoutManager(this));
        recyclerView.addItemDecoration(new CommentItemDecoration(this));
        recyclerView.setAdapter(new ThreadPreviewRecyclerViewAdapter(mItemManager, item));
        mVolumeNavigationDelegate.setScrollable(
                new VolumeNavigationDelegate.RecyclerViewHelper(recyclerView,
                        VolumeNavigationDelegate.RecyclerViewHelper.SCROLL_ITEM), null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mVolumeNavigationDelegate.attach(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVolumeNavigationDelegate.detach(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mVolumeNavigationDelegate.onKeyDown(keyCode, event) ||
                super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mVolumeNavigationDelegate.onKeyUp(keyCode, event) ||
                super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return mVolumeNavigationDelegate.onKeyLongPress(keyCode, event) ||
                super.onKeyLongPress(keyCode, event);
    }

    @Override
    protected boolean isDialogTheme() {
        return true;
    }
}
