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
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.UserManager;
import io.github.hidroh.materialistic.widget.CommentItemDecoration;
import io.github.hidroh.materialistic.widget.SubmissionRecyclerViewAdapter;

public class UserActivity extends InjectableActivity implements Scrollable {
    public static final String EXTRA_USERNAME = UserActivity.class.getName() + ".EXTRA_USERNAME";
    private static final String STATE_USER = "state:user";
    private static final String PARAM_ID = "id";
    @Inject UserManager mUserManager;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManger;
    private String mUsername;
    private UserManager.User mUser;
    private TextView mInfo;
    private TextView mAbout;
    private RecyclerView mRecyclerView;
    private TabLayout mTabLayout;
    private AppBarLayout mAppBar;
    private View mEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUsername = getIntent().getStringExtra(EXTRA_USERNAME);
        if (TextUtils.isEmpty(mUsername) && getIntent().getData() != null) {
            if (TextUtils.equals(getIntent().getData().getScheme(), BuildConfig.APPLICATION_ID)) {
                mUsername = getIntent().getData().getLastPathSegment();
            } else {
                mUsername = getIntent().getData().getQueryParameter(PARAM_ID);
            }
        }
        if (TextUtils.isEmpty(mUsername)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_user);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP);
        mAppBar = (AppBarLayout) findViewById(R.id.appbar);
        ((TextView) findViewById(R.id.title)).setText(mUsername);
        mInfo = (TextView) findViewById(R.id.user_info);
        mAbout = (TextView) findViewById(R.id.about);
        mEmpty = findViewById(R.id.empty);
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // no op
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // no op
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                scrollToTop();
            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        if (savedInstanceState != null) {
            mUser = savedInstanceState.getParcelable(STATE_USER);
        }
        if (mUser == null) {
            load();
        } else {
            bind();
        }
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_USER, mUser);
    }

    @Override
    public void scrollToTop() {
        mRecyclerView.smoothScrollToPosition(0);
        mAppBar.setExpanded(true, true);
    }

    private void load() {
        mUserManager.getUser(mUsername, new UserResponseListener(this));
    }

    private void onUserLoaded(UserManager.User response) {
        if (response != null) {
            mUser = response;
            bind();
        } else {
            showEmpty();
        }
    }

    private void showEmpty() {
        mInfo.setVisibility(View.GONE);
        mAbout.setVisibility(View.GONE);
        mEmpty.setVisibility(View.VISIBLE);
        mTabLayout.addTab(mTabLayout.newTab()
                .setText(getResources().getQuantityString(R.plurals.submissions_count, 0, "").trim()));
    }

    private void bind() {
        mInfo.setText(getString(R.string.user_info, mUser.getCreated(this), mUser.getKarma()));
        if (TextUtils.isEmpty(mUser.getAbout())) {
            mAbout.setVisibility(View.GONE);
        } else {
            AppUtils.setHtmlText(mAbout, mUser.getAbout());
        }
        int count = mUser.getItems().length;
        mTabLayout.addTab(mTabLayout.newTab()
                .setText(getResources().getQuantityString(R.plurals.submissions_count, count, count)));
        mRecyclerView.setAdapter(new SubmissionRecyclerViewAdapter(mItemManger, mUser.getItems()));
        mRecyclerView.addItemDecoration(new CommentItemDecoration(this));
    }

    private static class UserResponseListener implements ResponseListener<UserManager.User> {
        private final WeakReference<UserActivity> mUserActivity;

        public UserResponseListener(UserActivity userActivity) {
            mUserActivity = new WeakReference<>(userActivity);
        }

        @Override
        public void onResponse(UserManager.User response) {
            if (mUserActivity.get() != null && !mUserActivity.get().isActivityDestroyed()) {
                mUserActivity.get().onUserLoaded(response);
            }
        }

        @Override
        public void onError(String errorMessage) {
            if (mUserActivity.get() != null && !mUserActivity.get().isActivityDestroyed()) {
                Toast.makeText(mUserActivity.get(), R.string.user_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
