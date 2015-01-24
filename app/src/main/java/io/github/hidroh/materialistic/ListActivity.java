package io.github.hidroh.materialistic;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
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

public class ListActivity extends ActionBarActivity {

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public int getOrientation() {
                return LinearLayout.VERTICAL;
            }
        });
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeLayout);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                bindData();
            }
        });
        bindData();
    }

    private void bindData() {
        HackerNewsClient.getInstance().getTopStories(new HackerNewsClient.ResponseListener<HackerNewsClient.TopStory[]>() {
            @Override
            public void onResponse(final HackerNewsClient.TopStory[] response) {
                mRecyclerView.setAdapter(new RecyclerView.Adapter<ItemViewHolder>(){
                    @Override
                    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        return new ItemViewHolder(getLayoutInflater().inflate(R.layout.activity_list_item, parent, false));
                    }

                    @Override
                    public void onBindViewHolder(final ItemViewHolder holder, final int position) {
                        final HackerNewsClient.TopStory story = response[position];
                        holder.mRankTextView.setText(String.valueOf(position + 1));
                        if (!TextUtils.isEmpty(story.getTitle())) {
                            bindViewHolder(holder, story);
                        } else {
                            bindViewHolder(holder, null);
                            HackerNewsClient.getInstance().getItem(String.valueOf(story.getId()),
                                    new HackerNewsClient.ResponseListener<HackerNewsClient.Item>() {
                                        @Override
                                        public void onResponse(HackerNewsClient.Item response) {
                                            story.populate(response);
                                            bindViewHolder(holder, story);
                                        }

                                        @Override
                                        public void onError(String errorMessage) {
                                            // do nothing
                                        }
                                    });
                        }
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final Intent intent = new Intent(ListActivity.this, ItemActivity.class);
                                intent.putExtra(ItemActivity.EXTRA_ID, String.valueOf(story.getId()));
                                startActivity(intent);
                            }
                        });
                    }

                    @Override
                    public int getItemCount() {
                        return response.length;
                    }

                    private void bindViewHolder(ItemViewHolder holder, HackerNewsClient.TopStory story) {
                        if (story == null) {
                            holder.mTitleTextView.setText(getString(R.string.loading_text));
                            holder.mPostedTextView.setText(getString(R.string.loading_text));
                            holder.mCommentButton.setVisibility(View.INVISIBLE);
                        } else {
                            holder.mTitleTextView.setText(story.getTitle());
                            holder.mPostedTextView.setText(story.getDisplayedTime(ListActivity.this));
                            if (story.getKidCount() > 0) {
                                holder.mCommentButton.setText(String.valueOf(story.getKidCount()));
                                holder.mCommentButton.setVisibility(View.VISIBLE);
                            } else {
                                holder.mCommentButton.setVisibility(View.INVISIBLE);
                            }
                        }
                    }
                });
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("tag", errorMessage);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list, menu);
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

    private class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView mTitleTextView;
        private TextView mRankTextView;
        private TextView mPostedTextView;
        private Button mCommentButton;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mRankTextView = (TextView) itemView.findViewById(android.R.id.text1);
            mTitleTextView = (TextView) itemView.findViewById(android.R.id.text2);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mCommentButton = (Button) itemView.findViewById(R.id.comment);
        }
    }
}
