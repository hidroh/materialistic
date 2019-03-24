package io.github.hidroh.materialistic;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import androidx.annotation.IdRes;
import androidx.annotation.MenuRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SearchView;
import android.view.MenuItem;
import android.view.View;

import org.robolectric.RuntimeEnvironment;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.appwidget.WidgetConfigActivity;
import io.github.hidroh.materialistic.appwidget.WidgetConfigActivityTest;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.FavoriteManagerTest;
import io.github.hidroh.materialistic.data.FeedbackClient;
import io.github.hidroh.materialistic.data.FileDownloader;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ItemSyncJobServiceTest;
import io.github.hidroh.materialistic.data.ItemSyncService;
import io.github.hidroh.materialistic.data.MaterialisticDatabase;
import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.data.RestServiceFactory;
import io.github.hidroh.materialistic.data.SessionManager;
import io.github.hidroh.materialistic.data.SyncDelegate;
import io.github.hidroh.materialistic.data.SyncScheduler;
import io.github.hidroh.materialistic.data.UserManager;
import io.github.hidroh.materialistic.test.InMemoryDatabase;
import io.github.hidroh.materialistic.test.TestFavoriteActivity;
import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestReadabilityActivity;
import io.github.hidroh.materialistic.test.WebActivity;
import io.github.hidroh.materialistic.widget.FavoriteRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.MultiPageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.PopupMenu;
import io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.StoryRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.SubmissionRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.ThreadPreviewRecyclerViewAdapter;
import okhttp3.Call;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Module(
        injects = {
                // source classes
                LoginActivity.class,
                SettingsActivity.class,
                AskActivity.class,
                AboutActivity.class,
                FavoriteActivity.class,
                FeedbackActivity.class,
                ItemActivity.class,
                JobsActivity.class,
                ListActivity.class,
                BestActivity.class,
                NewActivity.class,
                SearchActivity.class,
                ShowActivity.class,
                WebActivity.class,
                PopularActivity.class,
                ComposeActivity.class,
                SubmitActivity.class,
                UserActivity.class,
                ThreadPreviewActivity.class,
                WidgetConfigActivity.class,
                FavoriteFragment.class,
                ItemFragment.class,
                ListFragment.class,
                WebFragment.class,
                ReleaseNotesActivity.class,
                StoryRecyclerViewAdapter.class,
                FavoriteRecyclerViewAdapter.class,
                SinglePageItemRecyclerViewAdapter.class,
                MultiPageItemRecyclerViewAdapter.class,
                SubmissionRecyclerViewAdapter.class,
                ThreadPreviewRecyclerViewAdapter.class,
                ItemSyncService.class,
                // test classes
                AppUtilsTest.class,
                SettingsActivityTest.class,
                SearchActivityTest.class,
                ItemActivityTest.class,
                TestReadabilityActivity.class,
                TestListActivity.class,
                io.github.hidroh.materialistic.test.ListActivity.class,
                FavoriteActivityTest.class,
                FavoriteActivityEmptyTest.class,
                FavoriteManagerTest.class,
                TestFavoriteActivity.class,
                WebFragmentLocalTest.class,
                WebFragmentTest.class,
                FeedbackActivityTest.class,
                PopularActivityTest.class,
                ReadabilityFragmentLazyLoadTest.class,
                DrawerActivityLoginTest.class,
                ComposeActivityTest.class,
                SubmitActivityTest.class,
                UserActivityTest.class,
                ThreadPreviewActivityTest.class,
                WidgetConfigActivityTest.class,
                PreferencesActivityTest.class,
                ItemSyncJobServiceTest.TestItemSyncJobService.class
        },
        library = true,
        overrides = true
)
public class TestActivityModule {
    private final ItemManager hackerNewsClient = mock(ItemManager.class);
    private final ItemManager algoliaClient = mock(ItemManager.class);
    private final ItemManager algoliaPopularClient = mock(ItemManager.class);
    private final UserManager userManager = mock(UserManager.class);
    private final FavoriteManager favoriteManager = mock(FavoriteManager.class);
    private final SessionManager sessionManager = mock(SessionManager.class);
    private final SearchView searchView = mock(SearchView.class);
    private final FeedbackClient feedbackClient = mock(FeedbackClient.class);
    private final ReadabilityClient readabilityClient = mock(ReadabilityClient.class);
    private final UserServices userServices = mock(UserServices.class);
    private final CustomTabsDelegate customTabsDelegate = mock(CustomTabsDelegate.class);
    private final KeyDelegate keyDelegate = mock(KeyDelegate.class);
    private final RestServiceFactory restServiceFactory = mock(RestServiceFactory.class);
    private final ResourcesProvider resourcesProvider = mock(ResourcesProvider.class);
    private final SyncDelegate syncDelegate = mock(SyncDelegate.class);
    private final Call.Factory callFactory = mock(Call.Factory.class);
    private final FileDownloader fileDownloader = mock(FileDownloader.class);
    private final SyncScheduler syncScheduler = mock(SyncScheduler.class);
    {
        TypedArray typedArray = mock(TypedArray.class);
        when(typedArray.length()).thenReturn(1);
        when(typedArray.getColor(anyInt(), anyInt())).thenReturn(0);
        when(resourcesProvider.obtainTypedArray(anyInt())).thenReturn(typedArray);
    }

    @Provides @Singleton @Named(ActivityModule.HN)
    public ItemManager provideHackerNewsClient() {
        return hackerNewsClient;
    }

    @Provides @Singleton @Named(ActivityModule.ALGOLIA)
    public ItemManager provideAlgoliaClient() {
        return algoliaClient;
    }

    @Provides @Singleton @Named(ActivityModule.POPULAR)
    public ItemManager provideAlgoliaPopularClient() {
        return algoliaPopularClient;
    }

    @Provides @Singleton
    public FavoriteManager provideFavoriteManager() {
        return favoriteManager;
    }

    @Provides @Singleton
    public SessionManager provideSessionManager() {
        return sessionManager;
    }

    @Provides @Singleton
    public FeedbackClient provideFeedbackClient() {
        return feedbackClient;
    }

    @Provides @Singleton
    public ReadabilityClient provideReadabilityClient() {
        return readabilityClient;
    }

    @Provides @Singleton
    public UserManager provideUserManager() {
        return userManager;
    }

    @Provides @Singleton
    public RestServiceFactory provideRestServiceFactory() {
        return restServiceFactory;
    }

    @Provides @Singleton
    public FileDownloader provideFileDownloader() {
        return fileDownloader;
    }

    @Provides @Singleton
    public Call.Factory provideCallFactory() {
        return callFactory;
    }

    @Provides @Singleton
    public ActionViewResolver provideActionViewResolver() {
        ActionViewResolver resolver = mock(ActionViewResolver.class);
        when(resolver.getActionView(any(MenuItem.class))).thenReturn(searchView);
        return resolver;
    }

    @Provides
    public AlertDialogBuilder provideAlertDialogBuilder() {
        return new AlertDialogBuilder() {
            private AlertDialog.Builder builder;

            @Override
            public AlertDialogBuilder init(Context context) {
                builder = new AlertDialog.Builder(context);
                return this;
            }

            @Override
            public AlertDialogBuilder setTitle(int titleId) {
                builder.setTitle(titleId);
                return this;
            }

            @Override
            public AlertDialogBuilder setMessage(@StringRes int messageId) {
                builder.setMessage(messageId);
                return this;
            }

            @Override
            public AlertDialogBuilder setView(View view) {
                builder.setView(view);
                return this;
            }

            @Override
            public AlertDialogBuilder setSingleChoiceItems(CharSequence[] items, int checkedItem, DialogInterface.OnClickListener listener) {
                builder.setSingleChoiceItems(items, checkedItem, listener);
                return this;
            }

            @Override
            public AlertDialogBuilder setNegativeButton(@StringRes int textId,
                                                        DialogInterface.OnClickListener listener) {
                builder.setNegativeButton(textId, listener);
                return this;
            }

            @Override
            public AlertDialogBuilder setPositiveButton(@StringRes int textId,
                                                        DialogInterface.OnClickListener listener) {
                builder.setPositiveButton(textId, listener);
                return this;
            }

            @Override
            public AlertDialogBuilder setNeutralButton(@StringRes int textId, DialogInterface.OnClickListener listener) {
                builder.setNeutralButton(textId, listener);
                return this;
            }

            @Override
            public Dialog create() {
                return builder.create();
            }

            @Override
            public Dialog show() {
                return builder.show();
            }
        };
    }

    @Provides @Singleton
    public UserServices provideUserServices() {
        return userServices;
    }

    @Provides
    public AccountManager provideAccountManager() {
        return AccountManager.get(RuntimeEnvironment.application);
    }

    @Provides @Singleton
    public KeyDelegate provideVolumeNavigationDelegate() {
        return keyDelegate;
    }

    @Provides
    public PopupMenu providePopupMenu() {
        return new PopupMenu() {
            private android.widget.PopupMenu popupMenu;

            @SuppressLint("NewApi")
            @Override
            public PopupMenu create(Context context, View anchor, int gravity) {
                popupMenu = new android.widget.PopupMenu(context,
                        anchor == null ? new View(context) : anchor, gravity);
                return this;
            }

            @SuppressLint("NewApi")
            @Override
            public PopupMenu inflate(@MenuRes int menuRes) {
                popupMenu.inflate(menuRes);
                return this;
            }

            @SuppressLint("NewApi")
            @Override
            public PopupMenu setMenuItemVisible(@IdRes int itemResId, boolean visible) {
                popupMenu.getMenu().findItem(itemResId).setVisible(visible);
                return this;
            }

            @SuppressLint("NewApi")
            @Override
            public PopupMenu setMenuItemTitle(@IdRes int itemResId, @StringRes int title) {
                popupMenu.getMenu().findItem(itemResId).setTitle(title);
                return this;
            }

            @SuppressLint("NewApi")
            @Override
            public PopupMenu setOnMenuItemClickListener(final OnMenuItemClickListener listener) {
                popupMenu.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return listener.onMenuItemClick(item);
                    }
                });
                return this;
            }

            @SuppressLint("NewApi")
            @Override
            public void show() {
                popupMenu.show();
            }
        };
    }

    @Provides @Singleton
    public CustomTabsDelegate provideCustomTabsDelegate() {
        return customTabsDelegate;
    }

    @Provides @Singleton
    public ResourcesProvider provideResourcesProvider() {
        return resourcesProvider;
    }

    @Provides @Singleton
    public SyncDelegate provideSyncDelegate() {
        return syncDelegate;
    }

    @Provides @Singleton
    public SyncScheduler provideSyncScheduler() {
        return syncScheduler;
    }

    @Provides @Singleton @Named(DataModule.MAIN_THREAD)
    public Scheduler provideMainThreadScheduler() {
        return Schedulers.immediate();
    }

    @Provides @Singleton @Named(DataModule.IO_THREAD)
    public Scheduler provideIoThreadScheduler() {
        return Schedulers.immediate();
    }

    @Provides
    public MaterialisticDatabase provideDatabase() {
        return InMemoryDatabase.getInstance(RuntimeEnvironment.application);
    }

    @Provides
    public MaterialisticDatabase.SavedStoriesDao provideSavedStoriesDao(MaterialisticDatabase database) {
        return database.getSavedStoriesDao();
    }

    @Provides
    public MaterialisticDatabase.ReadStoriesDao provideReadStoriesDao(MaterialisticDatabase database) {
        return database.getReadStoriesDao();
    }

    @Provides
    public MaterialisticDatabase.ReadableDao provideReadableDao(MaterialisticDatabase database) {
        return database.getReadableDao();
    }

    @Provides
    public SupportSQLiteOpenHelper provideOpenHelper(MaterialisticDatabase database) {
        return database.getOpenHelper();
    }
}
