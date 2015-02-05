package io.github.hidroh.materialistic;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import io.github.hidroh.materialistic.data.HackerNewsClient;

public class ListActivity extends BaseActivity {

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
}
