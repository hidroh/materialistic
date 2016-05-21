package io.github.hidroh.materialistic.data;

import android.annotation.SuppressLint;

@SuppressLint("ParcelCreator")
public class TestHnItem extends HackerNewsItem {
    public TestHnItem(long id) {
        super(id);
    }

    public TestHnItem(long id, int level) {
        this(id);
        this.level = level;
    }
}
