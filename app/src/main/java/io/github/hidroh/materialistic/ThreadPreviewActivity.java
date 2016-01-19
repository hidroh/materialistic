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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.Window;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.widget.CommentItemDecoration;
import io.github.hidroh.materialistic.widget.ThreadPreviewRecyclerViewAdapter;

public class ThreadPreviewActivity extends InjectableActivity {
    public static final String EXTRA_ITEM = ThreadPreviewActivity.class.getName() + ".EXTRA_ITEM";

    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Item item = getIntent().getParcelableExtra(EXTRA_ITEM);
        if (item == null) {
            finish();
            return;
        }
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_thread_preview);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new CommentItemDecoration(this));
        recyclerView.setAdapter(new ThreadPreviewRecyclerViewAdapter(mItemManager, item));
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
    protected boolean isDialogTheme() {
        return true;
    }
}
