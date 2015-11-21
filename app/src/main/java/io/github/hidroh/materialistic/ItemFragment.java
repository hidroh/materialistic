package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.widget.ItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.MultiPageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter;

public class ItemFragment extends LazyLoadFragment implements Scrollable {

    public static final String EXTRA_ITEM = ItemFragment.class.getName() + ".EXTRA_ITEM";
    private static final String STATE_ITEM = "state:item";
    private static final String STATE_ITEM_ID = "state:itemId";
    private static final String STATE_SINGLE_PAGE = "state:singlePage";
    private static final String STATE_ADAPTER_ITEMS = "state:adapterItems";
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private ItemManager.Item mItem;
    private String mItemId;
    private boolean mIsResumed;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean mSinglePage;
    private SinglePageItemRecyclerViewAdapter.SavedState mAdapterItems;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mItem = savedInstanceState.getParcelable(STATE_ITEM);
            mItemId = savedInstanceState.getString(STATE_ITEM_ID);
            mSinglePage = savedInstanceState.getBoolean(STATE_SINGLE_PAGE);
            mAdapterItems = savedInstanceState.getParcelable(STATE_ADAPTER_ITEMS);
        } else {
            ItemManager.WebItem item = getArguments().getParcelable(EXTRA_ITEM);
            if (item instanceof ItemManager.Item) {
                mItem = (ItemManager.Item) item;
            }
            mItemId = item != null ? item.getId() : null;
            mSinglePage = Preferences.isDefaultSinglePageComments(getActivity());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final View view = getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_item, container, false);
        mEmptyView = view.findViewById(android.R.id.empty);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()) {
            @Override
            public int getOrientation() {
                return LinearLayout.VERTICAL;
            }
        });
        mRecyclerView.setHasFixedSize(true);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.white);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.redA200);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (TextUtils.isEmpty(mItemId)) {
                    return;
                }

                loadKidData();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsResumed = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_SINGLE_PAGE, mSinglePage);
        outState.putParcelable(STATE_ITEM, mItem);
        outState.putString(STATE_ITEM_ID, mItemId);
        outState.putParcelable(STATE_ADAPTER_ITEMS, mAdapterItems);
    }

    @Override
    protected void load() {
        if (mItem != null) {
            bindKidData();
        } else if (!TextUtils.isEmpty(mItemId)) {
            loadKidData();
        }
    }

    @Override
    public void onPause() {
        mIsResumed = false;
        super.onPause();
    }

    @Override
    public void scrollToTop() {
        mRecyclerView.smoothScrollToPosition(0);
    }

    private void loadKidData() {
        mItemManager.getItem(mItemId, new ItemManager.ResponseListener<ItemManager.Item>() {
            @Override
            public void onResponse(ItemManager.Item response) {
                if (!mIsResumed) {
                    return;
                }

                mSwipeRefreshLayout.setRefreshing(false);
                mAdapterItems = null;
                mItem = response;
                bindKidData();
            }

            @Override
            public void onError(String errorMessage) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void bindKidData() {
        if (mItem == null || mItem.getKidCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            return;
        }

        final RecyclerView.Adapter<? extends ItemRecyclerViewAdapter.ItemViewHolder> adapter;
        if (mSinglePage) {
            if (mAdapterItems == null) {
                mAdapterItems = new SinglePageItemRecyclerViewAdapter.SavedState(
                        new ArrayList<>(Arrays.asList(mItem.getKidItems())));
            }
            adapter = new SinglePageItemRecyclerViewAdapter(mItemManager, mAdapterItems);
        } else {
            adapter = new MultiPageItemRecyclerViewAdapter(mItemManager, mItem.getKidItems(),
                    getArguments().getInt(ItemActivity.EXTRA_ITEM_LEVEL, 0));
        }
        mRecyclerView.setAdapter(adapter);
    }

}
