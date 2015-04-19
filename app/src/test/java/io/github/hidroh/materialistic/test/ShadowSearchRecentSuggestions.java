package io.github.hidroh.materialistic.test;

import android.provider.SearchRecentSuggestions;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;

@Implements(value = SearchRecentSuggestions.class, inheritImplementationMethods = true)
public class ShadowSearchRecentSuggestions {
    public static int historyClearCount = 0;
    public static List<String> recentQueries = new ArrayList<>();

    @Implementation
    public void clearHistory() {
        historyClearCount++;
    }

    @Implementation
    public void saveRecentQuery(final String queryString, final String line2) {
        recentQueries.add(queryString);
    }
}