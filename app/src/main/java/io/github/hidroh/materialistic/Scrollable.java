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

/**
 * Interface for things that can be scrolled vertically
 */
public interface Scrollable {
    /**
     * Scrolls this instance to top, i.e. until no more content above
     */
    void scrollToTop();

    /**
     * Scrolls to reveal more content below current content
     * @return  true if successful, false if unable to scroll
     */
    boolean scrollToNext();

    /**
     * Scrolls to reveal more content above current content
     * @return  true if successful, false if unable to scroll
     */
    boolean scrollToPrevious();
}
