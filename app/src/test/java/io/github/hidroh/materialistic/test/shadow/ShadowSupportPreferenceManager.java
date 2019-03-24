/*
 * Copyright (c) 2016 Ha Duy Trung
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

package io.github.hidroh.materialistic.test.shadow;

import android.content.res.Resources;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceManager;

import org.robolectric.annotation.Implements;

@Implements(PreferenceManager.class)
public class ShadowSupportPreferenceManager {

    public static int getPreferencePosition(PreferenceGroupAdapter adapter,
                                            Class<? extends Preference> clazz) {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (clazz.isInstance(adapter.getItem(i))) {
                return i;
            }
        }
        throw new Resources.NotFoundException();
    }
}
