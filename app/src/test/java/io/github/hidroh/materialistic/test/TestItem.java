package io.github.hidroh.materialistic.test;

import android.content.Context;
import android.os.Parcel;

import io.github.hidroh.materialistic.data.ItemManager;

public abstract class TestItem implements ItemManager.Item {
    @Override
    public void populate(ItemManager.Item info) {

    }

    @Override
    public String getRawType() {
        return null;
    }

    @Override
    public String getRawUrl() {
        return null;
    }

    @Override
    public long[] getKids() {
        return new long[0];
    }

    @Override
    public String getBy() {
        return null;
    }

    @Override
    public long getTime() {
        return 0;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public String getText() {
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
    public int getKidCount() {
        return 0;
    }

    @Override
    public ItemManager.Item[] getKidItems() {
        return new ItemManager.Item[0];
    }

    @Override
    public boolean isFavorite() {
        return false;
    }

    @Override
    public void setFavorite(boolean favorite) {

    }

    @Override
    public int getLocalRevision() {
        return 0;
    }

    @Override
    public void setLocalRevision(int localRevision) {

    }

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
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
