package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.UserManager;
import io.github.hidroh.materialistic.widget.UserItemRecyclerViewAdapter;

public class UserActivity extends InjectableActivity implements Scrollable {
    public static final String EXTRA_USERNAME = UserActivity.class.getName() + ".EXTRA_USERNAME";
    private static final String STATE_USER = "state:user";
    @Inject UserManager mUserManager;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManger;
    private String mUsername;
    private UserManager.User mUser;
    private TextView mTitle;
    private TextView mInfo;
    private TextView mAbout;
    private RecyclerView mRecyclerView;
    private UserItemRecyclerViewAdapter mAdapter;
    private TabLayout mTabLayout;
    private AppBarLayout mAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUsername = getIntent().getStringExtra(EXTRA_USERNAME);
        if (TextUtils.isEmpty(mUsername)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_user);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP);
        mAppBar = (AppBarLayout) findViewById(R.id.appbar);
        mTitle = (TextView) findViewById(R.id.title);
        mInfo = (TextView) findViewById(R.id.user_info);
        mAbout = (TextView) findViewById(R.id.about);
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
        mUserManager.getUser(mUsername, new ResponseListener<UserManager.User>() {
            @Override
            public void onResponse(UserManager.User response) {
                if (response != null) {
                    mUser = response;
                    bind();
                } else {
                    // TODO
                }
            }

            @Override
            public void onError(String errorMessage) {
                // TODO
            }
        });
    }

    private void bind() {
        mTitle.setText(mUser.getId());
        mInfo.setText(getString(R.string.user_info,
                DateUtils.formatDateTime(this, mUser.getCreated() * 1000, DateUtils.FORMAT_SHOW_DATE),
                mUser.getKarma()));
        AppUtils.setHtmlText(mAbout, mUser.getAbout());
        mTabLayout.addTab(mTabLayout.newTab()
                .setText(getString(R.string.submissions_count, mUser.getItems().length)));
        mAdapter = new UserItemRecyclerViewAdapter(mItemManger, mUser.getItems());
        mRecyclerView.setAdapter(mAdapter);
    }
}
