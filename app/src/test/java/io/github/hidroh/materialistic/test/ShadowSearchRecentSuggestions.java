package io.github.hidroh.materialistic.test;

import android.provider.SearchRecentSuggestions;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(value = SearchRecentSuggestions.class, inheritImplementationMethods = true)
public class ShadowSearchRecentSuggestions {
    public static int historyClearCount = 0;

    @Implementation
    public void clearHistory() {
        historyClearCount++;
    }
}
