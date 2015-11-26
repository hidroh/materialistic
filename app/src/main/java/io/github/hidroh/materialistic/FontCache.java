package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.support.v4.util.ArrayMap;
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
