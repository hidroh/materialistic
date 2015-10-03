package io.github.hidroh.materialistic.test;

import android.os.Bundle;

import io.github.hidroh.materialistic.InjectableActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.WebFragment;
import io.github.hidroh.materialistic.data.ItemManager;

public class WebActivity extends InjectableActivity {
    public static final String EXTRA_ITEM = WebActivity.class.getName() + ".EXTRA_ITEM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        ItemManager.WebItem item = getIntent().getParcelableExtra(EXTRA_ITEM);
        getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content,
                        WebFragment.instantiate(this, item),
                        WebFragment.class.getName())
                .commit();
    }
}
