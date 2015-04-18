package io.github.hidroh.materialistic;

/**
 * Callback interface for item observers
 */
public interface ItemObserver {
    /**
     * Fired when number of item's kids has been updated
     * @param kidCount  number of item's kids
     */
    void onKidChanged(int kidCount);
}
