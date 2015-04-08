package io.github.hidroh.materialistic.test;

import android.content.Context;
import android.os.Parcel;

import io.github.hidroh.materialistic.data.ItemManager;

public abstract class TestWebItem implements ItemManager.WebItem {
    @Override
    public String getDisplayedTitle() {
        return null;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public boolean isShareable() {
        return false;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getSource() {
        return null;
    }

    @Override
    public CharSequence getDisplayedTime(Context context) {
        return null;
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
