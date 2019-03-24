/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hidroh.materialistic;

import androidx.annotation.Nullable;

import io.github.hidroh.materialistic.data.WebItem;

/**
 * Interface for multi-pane view events
 */
public interface MultiPaneListener {
    /**
     * Fired when an item has been selected in list view when multi-pane is active
     * @param item          selected item or null if selection is clear
     *
     */
    void onItemSelected(@Nullable WebItem item);

    /**
     * Gets item that has been opened via {@link #onItemSelected(WebItem)}
     * @return  opened item or null
     */
    WebItem getSelectedItem();

    /**
     * Checks if multi pane configuration is active
     * @return true if multi pane, false if single pane
     */
    boolean isMultiPane();
}
