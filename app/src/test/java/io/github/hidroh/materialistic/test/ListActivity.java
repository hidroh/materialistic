package io.github.hidroh.materialistic.test;

import android.support.v4.app.FragmentActivity;
import android.view.View;

import io.github.hidroh.materialistic.MultiPaneListener;
import io.github.hidroh.materialistic.data.ItemManager;

import static org.mockito.Mockito.mock;

public class ListActivity extends FragmentActivity implements MultiPaneListener {
    public MultiPaneListener multiPaneListener = mock(MultiPaneListener.class);

    @Override
    public void onItemSelected(ItemManager.WebItem story, View sharedElement) {
        multiPaneListener.onItemSelected(story, sharedElement);
    }

    @Override
    public void clearSelection() {
        multiPaneListener.clearSelection();
    }

    @Override
    public ItemManager.WebItem getSelectedItem() {
        return multiPaneListener.getSelectedItem();
    }
}
