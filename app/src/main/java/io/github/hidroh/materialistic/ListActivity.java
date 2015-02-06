package io.github.hidroh.materialistic;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;

public class ListActivity extends BaseActivity implements ListFragment.ItemOpenListener {

    private boolean mIsMultiPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
        super.onCreate(savedInstanceState);
        // delay setting title here to allow launcher to get app name
        setTitle(getString(R.string.title_activity_list));
        createView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mIsMultiPane != getResources().getBoolean(R.bool.multi_pane)) {
            createView();
        }
    }

    private void createView() {
        mIsMultiPane = getResources().getBoolean(R.bool.multi_pane);
        mContentView.removeAllViews();
        if (mIsMultiPane) {
            setContentView(R.layout.activity_list_land);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.list,
                            ListFragment.instantiate(this, HackerNewsClient.getInstance(this)),
                            ListFragment.class.getName())
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame,
                            ListFragment.instantiate(this, HackerNewsClient.getInstance(this)),
                            ListFragment.class.getName())
                    .commit();
        }
    }

    @Override
    public void onItemOpen(ItemManager.Item story) {
        findViewById(R.id.empty).setVisibility(View.GONE);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.content,
                        WebFragment.instantiate(this, story),
                        WebFragment.class.getName())
                .commit();
    }
}
