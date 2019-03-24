package io.github.hidroh.materialistic.test;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import io.github.hidroh.materialistic.InjectableActivity;
import io.github.hidroh.materialistic.R;

public class TestReadabilityActivity extends InjectableActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }
}
