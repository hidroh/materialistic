package io.github.hidroh.materialistic;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.SessionManager;
import io.github.hidroh.materialistic.test.TestFavoriteActivity;
import io.github.hidroh.materialistic.test.TestInjectableActivity;
import io.github.hidroh.materialistic.test.TestListActivity;

import static org.mockito.Mockito.mock;

@Module(
        injects = {
                // source classes
                ActionBarSettingsActivity.class, // TODO remove
                AskActivity.class,
                FavoriteActivity.class,
                ItemActivity.class,
                JobsActivity.class,
                ListActivity.class,
                NewActivity.class,
                SearchActivity.class,
                ShowActivity.class,
                WebActivity.class,
                FavoriteFragment.class,
                ItemFragment.class,
                ListFragment.class,
                WebFragment.class,
                // test classes
                ItemActivityTest.class,
                ItemFragmentTest.class,
                TestInjectableActivity.class,
                TestListActivity.class,
                io.github.hidroh.materialistic.test.ListActivity.class,
                ListFragmentViewHolderTest.class,
                FavoriteActivityTest.class,
                FavoriteActivityEmptyTest.class,
                TestFavoriteActivity.class,
                WebActivityLocalTest.class
        },
        library = true,
        overrides = true
)
public class TestActivityModule {
    private final ItemManager hackerNewsClient = mock(ItemManager.class);
    private final ItemManager algoliaClient = mock(ItemManager.class);
    private final FavoriteManager favoriteManager = mock(FavoriteManager.class);
    private final SessionManager sessionManager = mock(SessionManager.class);

    @Provides @Singleton @Named(ActivityModule.HN)
    public ItemManager provideHackerNewsClient() {
        return hackerNewsClient;
    }

    @Provides @Singleton @Named(ActivityModule.ALGOLIA)
    public ItemManager provideAlgoliaClient() {
        return algoliaClient;
    }

    @Provides @Singleton
    public FavoriteManager provideFavoriteManager() {
        return favoriteManager;
    }

    @Provides @Singleton
    public SessionManager provideSessionManager() {
        return sessionManager;
    }
}
