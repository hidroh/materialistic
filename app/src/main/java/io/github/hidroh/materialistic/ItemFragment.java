package io.github.hidroh.materialistic;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import io.github.hidroh.materialistic.widget.MultiPageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter;

public class ItemFragment extends BaseFragment implements Scrollable {

    private static final String EXTRA_ITEM = ItemFragment.class.getName() + ".EXTRA_ITEM";
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private ItemManager.Item mItem;
    private String mItemId;
    private boolean mIsResumed;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean mSinglePage;

    /**
     * Instantiates fragment to display given item
     * @param context   an instance of {@link android.content.Context}
     * @param item      item to display
     * @param args      fragment arguments or null
     * @return  item fragment
     */
    public static ItemFragment instantiate(Context context, ItemManager.WebItem item, Bundle args) {
        final ItemFragment fragment = (ItemFragment) Fragment.instantiate(context,
                ItemFragment.class.getName(), args == null ? new Bundle() : args);
        if (item instanceof ItemManager.Item) {
            fragment.mItem = (ItemManager.Item) item;
        }
        fragment.mItemId = item.getId();
        fragment.mSinglePage = Preferences.isDefaultSinglePageComments(context);
        return fragment;
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
        mSwipeRefreshLayout.setColorSchemeResources(R.color.textColorPrimary);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorAccent);
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            final ItemManager.Item savedItem = savedInstanceState.getParcelable(EXTRA_ITEM);
            if (savedItem != null) {
                mItem = savedItem;
            }
        }

        if (mItem != null) {
            bindKidData(mItem.getKidItems());
        } else if (!TextUtils.isEmpty(mItemId)) {
            loadKidData();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsResumed = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_ITEM, mItem);
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
                mItem = response;
                bindKidData(mItem.getKidItems());
            }

            @Override
            public void onError(String errorMessage) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void bindKidData(final ItemManager.Item[] items) {
        if (items == null || items.length == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            return;
        }

        if (mSinglePage) {
            final ArrayList<ItemManager.Item> list = new ArrayList<>(Arrays.asList(items));
            mRecyclerView.setAdapter(new SinglePageItemRecyclerViewAdapter(mItemManager, list));
        } else {
            mRecyclerView.setAdapter(new MultiPageItemRecyclerViewAdapter(mItemManager, items,
                    getArguments().getInt(ItemActivity.EXTRA_ITEM_LEVEL, 0)));
        }
    }

}
