package io.github.hidroh.materialistic;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ItemActivity extends BaseActivity {

    public static final String EXTRA_STORY = ItemActivity.class.getName() + ".EXTRA_STORY";
    private static final String PARAM_ID = "id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);
        final Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            final String id = intent.getData().getQueryParameter(PARAM_ID);
            HackerNewsClient.getInstance().getItem(id, new HackerNewsClient.ResponseListener<HackerNewsClient.Item>() {
                @Override
                public void onResponse(HackerNewsClient.Item response) {
                    bindData(response);
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e("tag", errorMessage);
                }
            });
        } else {
            HackerNewsClient.TopStory story = intent.getParcelableExtra(EXTRA_STORY);
            bindData(story);
        }
    }

    private void bindData(HackerNewsClient.ItemInterface story) {
        if (story == null) {
            return;
        }

        final TextView titleTextView = (TextView) findViewById(android.R.id.text2);
        titleTextView.setMaxLines(Integer.MAX_VALUE);
        titleTextView.setText(story.getTitle());
        ((TextView) findViewById(R.id.posted)).setText(story.getDisplayedTime(this));
        if (story.getKidCount() > 0) {
            Button commentButton = (Button) findViewById(R.id.comment);
            commentButton.setText(String.valueOf(story.getKidCount()));
            commentButton.setVisibility(View.VISIBLE);
        }
    }

}
