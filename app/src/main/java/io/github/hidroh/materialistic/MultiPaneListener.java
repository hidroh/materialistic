package io.github.hidroh.materialistic;

import android.view.View;

import io.github.hidroh.materialistic.data.ItemManager;

/**
 * Interface for multi-pane view events
 */
public interface MultiPaneListener {
    /**
     * Fired when an item has been selected in list view when multi-pane is active
     * @param item          selected item
     * @param sharedElement item view to be used for shared element transition
     */
    void onItemSelected(ItemManager.WebItem item, View sharedElement);

    /**
     * Clears item selection
     */
    void clearSelection();

    /**
     * Gets item that has been opened via {@link #onItemSelected(ItemManager.WebItem, View)}
     * @return  opened item or null
     */
    ItemManager.WebItem getSelectedItem();
}
