package io.github.hidroh.materialistic;

import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import io.github.hidroh.materialistic.data.HackerNewsClient;

public class ListActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame,
                            ListFragment.instantiate(this, HackerNewsClient.getInstance(this)),
                            ListFragment.class.getName())
                    .commit();
        }
    }

}
