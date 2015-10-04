package io.github.hidroh.materialistic;

import io.github.hidroh.materialistic.data.ItemManager;

/**
 * Interface for multi-pane view events
 */
public interface MultiPaneListener {
    /**
     * Fired when an item has been selected in list view when multi-pane is active
     * @param item          selected item
     *
     */
    void onItemSelected(ItemManager.WebItem item);

    /**
     * Clears item selection
     */
    void clearSelection();

    /**
     * Gets item that has been opened via {@link #onItemSelected(ItemManager.WebItem)}
     * @return  opened item or null
     */
    ItemManager.WebItem getSelectedItem();
}
