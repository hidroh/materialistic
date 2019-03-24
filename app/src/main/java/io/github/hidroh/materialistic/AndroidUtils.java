/*
 * Copyright (c) 2017 Ha Duy Trung
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

/**
 * Straight copies of Android's static utilities
 */
public interface AndroidUtils {

    class TextUtils {

        /**
         * Returns true if a and b are equal, including if they are both null.
         * <p><i>Note: In platform versions 1.1 and earlier, this method only worked well if
         * both the arguments were instances of String.</i></p>
         * @param a first CharSequence to check
         * @param b second CharSequence to check
         * @return true if a and b are equal
         */
        public static boolean equals(CharSequence a, CharSequence b) {
            if (a == b) return true;
            int length;
            if (a != null && b != null && (length = a.length()) == b.length()) {
                if (a instanceof String && b instanceof String) {
                    return a.equals(b);
                } else {
                    for (int i = 0; i < length; i++) {
                        if (a.charAt(i) != b.charAt(i)) return false;
                    }
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns true if the string is null or 0-length.
         * @param str the string to be examined
         * @return true if str is null or zero length
         */
        public static boolean isEmpty(@Nullable CharSequence str) {
            return str == null || str.length() == 0;
        }
    }
}
