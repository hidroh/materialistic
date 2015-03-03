package io.github.hidroh.materialistic.data;

public class SearchRecentSuggestionsProvider extends android.content.SearchRecentSuggestionsProvider {
    public static final String PROVIDER_AUTHORITY = "io.github.hidroh.materialistic.recentprovider";
    public static final int MODE = DATABASE_MODE_QUERIES;

    public SearchRecentSuggestionsProvider() {
        setupSuggestions(PROVIDER_AUTHORITY, MODE);
    }
}
