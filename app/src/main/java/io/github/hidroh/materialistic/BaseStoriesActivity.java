package io.github.hidroh.materialistic;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.ItemManager;

public abstract class BaseStoriesActivity extends BaseListActivity {
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;

    @NonNull
    @ItemManager.FetchMode
    protected abstract String getFetchMode();

    @Override
    protected Fragment instantiateListFragment() {
        return ListFragment.instantiate(this, mItemManager, getFetchMode());
    }

    @Override
    protected boolean isItemOptionsMenuVisible() {
        return mSelectedItem != null;
    }
}
