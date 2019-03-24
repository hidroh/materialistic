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

package io.github.hidroh.materialistic.test;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import io.github.hidroh.materialistic.data.LocalCache;

public class InMemoryCache implements LocalCache {

    private final Map<String, String> readability = new HashMap<>();
    private final Map<String, Boolean> viewed = new HashMap<>();

    @Nullable
    @Override
    public String getReadability(String itemId) {
        return readability.get(itemId);
    }

    @Override
    public void putReadability(String itemId, String content) {
        readability.put(itemId, content);
    }

    @Override
    public boolean isViewed(String itemId) {
        return viewed.containsKey(itemId) ? viewed.get(itemId) : false;
    }

    @Override
    public void setViewed(String itemId) {
        viewed.put(itemId, true);
    }

    @Override
    public boolean isFavorite(String itemId) {
        return false;
    }
}
