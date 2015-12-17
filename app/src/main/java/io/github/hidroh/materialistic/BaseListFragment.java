package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public abstract class BaseListFragment extends BaseFragment implements Scrollable {
    private static final String STATE_CARD_VIEW = "state:cardView";

    protected RecyclerView mRecyclerView;
    protected boolean mCardView = true;
    private final SharedPreferences.OnSharedPreferenceChangeListener mListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {
                    mChanged = true;
                    mCardView = Preferences.isListItemCardView(getActivity());
                }
            };
    private boolean mChanged;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(mListener);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState == null) {
            mCardView = Preferences.isListItemCardView(getActivity());
        } else {
            mCardView = savedInstanceState.getBoolean(STATE_CARD_VIEW, true);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);
        final int margin = getResources().getDimensionPixelSize(R.dimen.margin);
        final int divider = getResources().getDimensionPixelSize(R.dimen.divider);
        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                       RecyclerView.State state) {
                if (mCardView) {
                    outRect.set(margin * 2,
                            parent.getChildAdapterPosition(view) == 0 ? margin * 2 : margin,
                            margin * 2,
                            margin);
                } else {
                    outRect.set(0, 0, 0, divider);
                }
            }
        });
        mRecyclerView.setAdapter(getAdapter());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChanged) {
            getAdapter().notifyDataSetChanged();
            mChanged = false;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_list_toggle);
        if (mCardView) {
            item.setTitle(R.string.compact_view);
            mMenuTintDelegate.setIcon(item, R.drawable.ic_view_stream_white_24dp);
        } else {
            item.setTitle(R.string.card_view);
            mMenuTintDelegate.setIcon(item, R.drawable.ic_view_agenda_white_24dp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != R.id.menu_list_toggle) {
            return super.onOptionsItemSelected(item);
        }
        mCardView = !mCardView;
        Preferences.setListItemCardView(getActivity(), mCardView);
        getActivity().supportInvalidateOptionsMenu();
        getAdapter().notifyDataSetChanged();
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_CARD_VIEW, mCardView);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(mListener);
    }

    @Override
    public void scrollToTop() {
        mRecyclerView.smoothScrollToPosition(0);
    }

    protected abstract RecyclerView.Adapter getAdapter();
}
