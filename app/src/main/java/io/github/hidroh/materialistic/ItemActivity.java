package io.github.hidroh.materialistic;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class ItemActivity extends BaseActivity {

    public static final String EXTRA_ID = ItemActivity.class.getName() + ".EXTRA_ID";
    private static final String PARAM_ID = "id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);
        final Intent intent = getIntent();
        final String id;
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            id = intent.getData().getQueryParameter(PARAM_ID);
        } else {
            id = intent.getStringExtra(EXTRA_ID);
        }
        HackerNewsClient.getInstance().getItem(id, new HackerNewsClient.ResponseListener<HackerNewsClient.Item>() {
            @Override
            public void onResponse(HackerNewsClient.Item response) {
                ((TextView) findViewById(android.R.id.text1)).setText(response.getTitle());
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("tag", errorMessage);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
