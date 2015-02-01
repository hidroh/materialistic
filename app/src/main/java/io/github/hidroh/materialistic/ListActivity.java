package io.github.hidroh.materialistic;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Window;

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
                            Fragment.instantiate(this, ListFragment.class.getName()),
                            ListFragment.class.getName())
                    .commit();
        }
    }

}
