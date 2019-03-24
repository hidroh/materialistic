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

import android.content.Context;
import android.graphics.Typeface;
import androidx.collection.ArrayMap;
import android.text.TextUtils;

public class FontCache {

    private static FontCache sInstance;
    private final ArrayMap<String, Typeface> mTypefaceMap = new ArrayMap<>();

    public static FontCache getInstance() {
        if (sInstance == null) {
            sInstance = new FontCache();
        }
        return sInstance;
    }

    private FontCache() { }

    public Typeface get(Context context, String typefaceName) {
        if (TextUtils.isEmpty(typefaceName)) {
            return null;
        }
        if (!mTypefaceMap.containsKey(typefaceName)) {
            mTypefaceMap.put(typefaceName, Typeface.createFromAsset(context.getAssets(), typefaceName));
        }
        return mTypefaceMap.get(typefaceName);
    }
}
