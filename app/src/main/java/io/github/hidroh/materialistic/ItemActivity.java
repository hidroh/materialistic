package io.github.hidroh.materialistic;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ItemActivity extends BaseItemActivity {

    public static final String EXTRA_STORY = ItemActivity.class.getName() + ".EXTRA_STORY";
    private static final String PARAM_ID = "id";
    private RecyclerView mRecyclerView;
    private String mItemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public int getOrientation() {
                return LinearLayout.VERTICAL;
            }
        });
        final Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            mItemId = intent.getData().getQueryParameter(PARAM_ID);
            HackerNewsClient.getInstance().getItem(mItemId, new HackerNewsClient.ResponseListener<HackerNewsClient.Item>() {
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
            HackerNewsClient.Item item = intent.getParcelableExtra(EXTRA_STORY);
            mItemId = String.valueOf(item.getId());
            bindData(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_external) {
            AppUtils.openWebUrlExternal(this, HackerNewsClient.getItemUrl(mItemId));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void bindData(HackerNewsClient.Item story) {
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
        bindListData(story.getKidItems());
    }

    private void bindListData(final HackerNewsClient.Item[] items) {
        if (items == null || items.length == 0) {
            return;
        }

        mRecyclerView.setAdapter(new RecyclerView.Adapter<ItemViewHolder>() {
            @Override
            public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new ItemViewHolder(getLayoutInflater()
                        .inflate(R.layout.activity_sub_item, parent, false));
            }

            @Override
            public void onBindViewHolder(final ItemViewHolder holder, int position) {
                final HackerNewsClient.Item item = items[position];
                if (TextUtils.isEmpty(item.getText())) {
                    HackerNewsClient.getInstance().getItem(String.valueOf(item.getId()),
                            new HackerNewsClient.ResponseListener<HackerNewsClient.Item>() {
                                @Override
                                public void onResponse(HackerNewsClient.Item response) {
                                    bindListItem(holder, response);
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    // do nothing
                                }
                            });
                } else {
                    bindListItem(holder, item);
                }
            }

            private void bindListItem(ItemViewHolder holder, HackerNewsClient.Item item) {
                if (item == null) {
                    holder.mPostedTextView.setText(getString(R.string.loading_text));
                    holder.mContentTextView.setText(getString(R.string.loading_text));
                    holder.mCommentButton.setVisibility(View.INVISIBLE);
                } else {
                    holder.mPostedTextView.setText(item.getDisplayedTime(ItemActivity.this));
                    holder.mContentTextView.setText(item.getText());
                    if (item.getKidCount() > 0) {
                        holder.mCommentButton.setText(String.valueOf(item.getKidCount()));
                        holder.mCommentButton.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public int getItemCount() {
                return items.length;
            }
        });
    }

    private static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView mPostedTextView;
        private final TextView mContentTextView;
        private final Button mCommentButton;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mContentTextView = (TextView) itemView.findViewById(android.R.id.text1);
            mCommentButton = (Button) itemView.findViewById(R.id.comment);
        }
    }
}
