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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import androidx.annotation.AttrRes;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSession;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.github.hidroh.materialistic.annotation.PublicApi;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.widget.PopupMenu;

@SuppressWarnings("WeakerAccess")
@PublicApi
public class AppUtils {
    private static final String ABBR_YEAR = "y";
    private static final String ABBR_WEEK = "w";
    private static final String ABBR_DAY = "d";
    private static final String ABBR_HOUR = "h";
    private static final String ABBR_MINUTE = "m";
    private static final String PLAY_STORE_URL = "market://details?id=" + BuildConfig.APPLICATION_ID;
    private static final String FORMAT_HTML_COLOR = "%06X";
    public static final int HOT_THRESHOLD_HIGH = 300;
    public static final int HOT_THRESHOLD_NORMAL = 100;
    static final int HOT_THRESHOLD_LOW = 10;
    public static final int HOT_FACTOR = 3;
    private static final String HOST_ITEM = "item";
    private static final String HOST_USER = "user";

    public static void openWebUrlExternal(Context context, @Nullable WebItem item,
                                          String url, @Nullable CustomTabsSession session) {
        if (!hasConnection(context)) {
            context.startActivity(new Intent(context, OfflineWebActivity.class)
                    .putExtra(OfflineWebActivity.EXTRA_URL, url));
            return;
        }
        Intent intent = createViewIntent(context, item, url, session);
        if (!HackerNewsClient.BASE_WEB_URL.contains(Uri.parse(url).getHost())) {
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            }
            return;
        }
        List<ResolveInfo> activities = context.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        ArrayList<Intent> intents = new ArrayList<>();
        for (ResolveInfo info : activities) {
            if (info.activityInfo.packageName.equalsIgnoreCase(context.getPackageName())) {
                continue;
            }
            intents.add(createViewIntent(context, item, url, session)
                    .setPackage(info.activityInfo.packageName));
        }
        if (intents.isEmpty()) {
            return;
        }
        if (intents.size() == 1) {
            context.startActivity(intents.remove(0));
        } else {
            context.startActivity(Intent.createChooser(intents.remove(0),
                    context.getString(R.string.chooser_title))
                    .putExtra(Intent.EXTRA_INITIAL_INTENTS,
                            intents.toArray(new Parcelable[intents.size()])));
        }
    }

    public static void setTextWithLinks(TextView textView, CharSequence html) {
        textView.setText(html);
        // TODO https://code.google.com/p/android/issues/detail?id=191430
        //noinspection Convert2Lambda
        textView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP ||
                        action == MotionEvent.ACTION_DOWN) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    TextView widget = (TextView) v;
                    x -= widget.getTotalPaddingLeft();
                    y -= widget.getTotalPaddingTop();

                    x += widget.getScrollX();
                    y += widget.getScrollY();

                    Layout layout = widget.getLayout();
                    int line = layout.getLineForVertical(y);
                    int off = layout.getOffsetForHorizontal(line, x);

                    ClickableSpan[] links = Spannable.Factory.getInstance()
                            .newSpannable(widget.getText())
                            .getSpans(off, off, ClickableSpan.class);

                    if (links.length != 0) {
                        if (action == MotionEvent.ACTION_UP) {
                            if (links[0] instanceof URLSpan) {
                                openWebUrlExternal(widget.getContext(), null,
                                        ((URLSpan) links[0]).getURL(), null);
                            } else {
                                links[0].onClick(widget);
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public static CharSequence fromHtml(String htmlText) {
        return fromHtml(htmlText, false);
    }

    public static CharSequence fromHtml(String htmlText, boolean compact) {
        if (TextUtils.isEmpty(htmlText)) {
            return null;
        }
        CharSequence spanned;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //noinspection InlinedApi
            spanned = Html.fromHtml(htmlText, compact ?
                    Html.FROM_HTML_MODE_COMPACT : Html.FROM_HTML_MODE_LEGACY);
        } else {
            //noinspection deprecation
            spanned = Html.fromHtml(htmlText);
        }
        return trim(spanned);
    }

    public static Intent makeSendIntentChooser(Context context, Uri data) {
        // use ACTION_SEND_MULTIPLE instead of ACTION_SEND to filter out
        // share receivers that accept only EXTRA_TEXT but not EXTRA_STREAM
        return Intent.createChooser(new Intent(Intent.ACTION_SEND_MULTIPLE)
                        .setType("text/plain")
                        .putParcelableArrayListExtra(Intent.EXTRA_STREAM,
                                new ArrayList<Uri>(){{add(data);}}),
                context.getString(R.string.share_file));
    }

    public static void openExternal(@NonNull final Context context,
                             @NonNull PopupMenu popupMenu,
                             @NonNull View anchor,
                             @NonNull final WebItem item,
                             final CustomTabsSession session) {
        if (TextUtils.isEmpty(item.getUrl()) ||
                item.getUrl().startsWith(HackerNewsClient.BASE_WEB_URL)) {
            openWebUrlExternal(context,
                    item, String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()),
                    session);
            return;
        }
        popupMenu.create(context, anchor, GravityCompat.END)
                .inflate(R.menu.menu_share)
                .setOnMenuItemClickListener(menuItem -> {
                    openWebUrlExternal(context, item, menuItem.getItemId() == R.id.menu_article ?
                            item.getUrl() :
                            String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()), session);
                    return true;
                })
                .show();
    }

    public static void share(@NonNull final Context context,
                             @NonNull PopupMenu popupMenu,
                             @NonNull View anchor,
                             @NonNull final WebItem item) {
        if (TextUtils.isEmpty(item.getUrl()) ||
                item.getUrl().startsWith(HackerNewsClient.BASE_WEB_URL)) {
            share(context, item.getDisplayedTitle(),
                    String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()));
            return;
        }
        popupMenu.create(context, anchor, GravityCompat.END)
                .inflate(R.menu.menu_share)
                .setOnMenuItemClickListener(menuItem -> {
                    share(context, item.getDisplayedTitle(),
                            menuItem.getItemId() == R.id.menu_article ?
                                    item.getUrl() :
                                    String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()));
                    return true;
                })
                .show();
    }

    public static int getThemedResId(Context context, @AttrRes int attr) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        final int resId = a.getResourceId(0, 0);
        a.recycle();
        return resId;
    }

    public static float getDimension(Context context, @StyleRes int styleResId, @AttrRes int attr) {
        TypedArray a = context.getTheme().obtainStyledAttributes(styleResId, new int[]{attr});
        float size = a.getDimension(0, 0);
        a.recycle();
        return size;
    }

    public static boolean isHackerNewsUrl(WebItem item) {
        return !TextUtils.isEmpty(item.getUrl()) &&
                item.getUrl().equals(String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()));
    }

    public static int getDimensionInDp(Context context, @DimenRes int dimenResId) {
        return (int) (context.getResources().getDimension(dimenResId) /
                        context.getResources().getDisplayMetrics().density);
    }

    public static void restart(Activity activity, boolean transition) {
        activity.recreate();
    }

    public static String getAbbreviatedTimeSpan(long timeMillis) {
        long span = Math.max(System.currentTimeMillis() - timeMillis, 0);
        if (span >= DateUtils.YEAR_IN_MILLIS) {
            return (span / DateUtils.YEAR_IN_MILLIS) + ABBR_YEAR;
        }
        if (span >= DateUtils.WEEK_IN_MILLIS) {
            return (span / DateUtils.WEEK_IN_MILLIS) + ABBR_WEEK;
        }
        if (span >= DateUtils.DAY_IN_MILLIS) {
            return (span / DateUtils.DAY_IN_MILLIS) + ABBR_DAY;
        }
        if (span >= DateUtils.HOUR_IN_MILLIS) {
            return (span / DateUtils.HOUR_IN_MILLIS) + ABBR_HOUR;
        }
        return (span / DateUtils.MINUTE_IN_MILLIS) + ABBR_MINUTE;
    }

    public static boolean isOnWiFi(Context context) {
        NetworkInfo activeNetwork = ((ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting() &&
                activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public static boolean hasConnection(Context context) {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    @SuppressLint("MissingPermission")
    public static Pair<String, String> getCredentials(Context context) {
        String username = Preferences.getUsername(context);
        if (TextUtils.isEmpty(username)) {
            return null;
        }
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(BuildConfig.APPLICATION_ID);
        for (Account account : accounts) {
            if (TextUtils.equals(username, account.name)) {
                return Pair.create(username, accountManager.getPassword(account));
            }
        }
        return null;
    }

    /**
     * Displays UI to allow user to login
     * If no accounts exist in user's device, regardless of login status, prompt to login again
     * If 1 or more accounts in user's device, and already logged in, prompt to update password
     * If 1 or more accounts in user's device, and logged out, show account chooser
     * @param context activity context
     * @param alertDialogBuilder dialog builder
     */
    @SuppressLint("MissingPermission")
    public static void showLogin(Context context, AlertDialogBuilder alertDialogBuilder) {
        Account[] accounts = AccountManager.get(context).getAccountsByType(BuildConfig.APPLICATION_ID);
        if (accounts.length == 0) { // no accounts, ask to login or re-login
            context.startActivity(new Intent(context, LoginActivity.class));
        } else if (!TextUtils.isEmpty(Preferences.getUsername(context))) { // stale account, ask to re-login
            context.startActivity(new Intent(context, LoginActivity.class));
        } else { // logged out, choose from existing accounts to log in
            showAccountChooser(context, alertDialogBuilder, accounts);
        }
    }

    @SuppressLint("MissingPermission")
    public static void registerAccountsUpdatedListener(final Context context) {
        AccountManager.get(context).addOnAccountsUpdatedListener(accounts -> {
            String username = Preferences.getUsername(context);
            if (TextUtils.isEmpty(username)) {
                return;
            }
            for (Account account : accounts) {
                if (TextUtils.equals(account.name, username)) {
                    return;
                }
            }
            Preferences.setUsername(context, null);
        }, null, true);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void openPlayStore(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL));
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_playstore, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    public static void showAccountChooser(final Context context, AlertDialogBuilder alertDialogBuilder,
                                           Account[] accounts) {
        String username = Preferences.getUsername(context);
        final String[] items = new String[accounts.length];
        int checked = -1;
        for (int i = 0; i < accounts.length; i++) {
            String accountName = accounts[i].name;
            items[i] = accountName;
            if (TextUtils.equals(accountName, username)) {
                checked = i;
            }
        }
        int initialSelection = checked;
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            private int selection = initialSelection;

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Preferences.setUsername(context, items[selection]);
                        Toast.makeText(context,
                                context.getString(R.string.welcome, items[selection]),
                                Toast.LENGTH_SHORT)
                                .show();
                        dialog.dismiss();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        Intent intent = new Intent(context, LoginActivity.class);
                        intent.putExtra(LoginActivity.EXTRA_ADD_ACCOUNT, true);
                        context.startActivity(intent);
                        dialog.dismiss();
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        if (selection < 0) {
                            break;
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            AccountManager.get(context).removeAccount(accounts[selection], null, null, null);
                        } else {
                            //noinspection deprecation
                            AccountManager.get(context).removeAccount(accounts[selection], null, null);
                        }
                        dialog.dismiss();
                        break;
                    default:
                        selection = which;
                        break;
                }
            }
        };
        alertDialogBuilder
                .init(context)
                .setTitle(R.string.choose_account)
                .setSingleChoiceItems(items, checked, clickListener)
                .setPositiveButton(android.R.string.ok, clickListener)
                .setNegativeButton(R.string.add_account, clickListener)
                .setNeutralButton(R.string.remove_account, clickListener)
                .show();
    }

    public static void toggleFab(FloatingActionButton fab, boolean visible) {
        if (visible) {
            fab.setTag(null);
            fab.show();
        } else {
            fab.setTag(FabAwareScrollBehavior.HIDDEN);
            fab.hide();
        }
    }

    public static void toggleFabAction(FloatingActionButton fab, WebItem item, boolean commentMode) {
        Context context = fab.getContext();
        fab.setImageResource(commentMode ? R.drawable.ic_reply_white_24dp : R.drawable.ic_zoom_out_map_white_24dp);
        fab.setOnClickListener(v -> {
            if (commentMode) {
                context.startActivity(new Intent(context, ComposeActivity.class)
                        .putExtra(ComposeActivity.EXTRA_PARENT_ID, item.getId())
                        .putExtra(ComposeActivity.EXTRA_PARENT_TEXT,
                                item instanceof Item ? ((Item) item).getText() : null));
            } else {
                LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(new Intent(WebFragment.ACTION_FULLSCREEN)
                                .putExtra(WebFragment.EXTRA_FULLSCREEN, true));
            }
        });
    }

    public static String toHtmlColor(Context context, @AttrRes int colorAttr) {
        return String.format(FORMAT_HTML_COLOR, 0xFFFFFF & ContextCompat.getColor(context,
                AppUtils.getThemedResId(context, colorAttr)));
    }

    public static void toggleWebViewZoom(WebSettings webSettings, boolean enabled) {
        webSettings.setSupportZoom(enabled);
        webSettings.setBuiltInZoomControls(enabled);
        webSettings.setDisplayZoomControls(false);
    }

    public static void setStatusBarDim(Window window, boolean dim) {
        setStatusBarColor(window, dim ? Color.TRANSPARENT :
                ContextCompat.getColor(window.getContext(),
                        AppUtils.getThemedResId(window.getContext(), R.attr.colorPrimaryDark)));
    }

    public static void setStatusBarColor(Window window, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(color);
        }
    }

    public static void navigate(int direction, AppBarLayout appBarLayout, Navigable navigable) {
        switch (direction) {
            case Navigable.DIRECTION_DOWN:
            case Navigable.DIRECTION_RIGHT:
                if (appBarLayout.getBottom() == 0) {
                    navigable.onNavigate(direction);
                } else {
                    appBarLayout.setExpanded(false, true);
                }
                break;
            default:
                navigable.onNavigate(direction);
                break;
        }
    }

    public static int getDisplayHeight(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        return point.y;
    }

    public static LayoutInflater createLayoutInflater(Context context) {
        return LayoutInflater.from(new ContextThemeWrapper(context,
                Preferences.Theme.resolvePreferredTextSize(context)));
    }

    public static void share(Context context, String subject, String text) {
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, !TextUtils.isEmpty(subject) ?
                        TextUtils.join(" - ", new String[]{subject, text}) : text);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }
    public static Uri createItemUri(@NonNull String itemId) {
        return new Uri.Builder()
                .scheme(BuildConfig.APPLICATION_ID)
                .authority(HOST_ITEM)
                .path(itemId)
                .build();
    }

    public static Uri createUserUri(@NonNull String userId) {
        return new Uri.Builder()
                .scheme(BuildConfig.APPLICATION_ID)
                .authority(HOST_USER)
                .path(userId)
                .build();
    }

    public static String getDataUriId(@NonNull Intent intent, String altParamId) {
        if (intent.getData() == null) {
            return null;
        }
        if (TextUtils.equals(intent.getData().getScheme(), BuildConfig.APPLICATION_ID)) {
            return intent.getData().getLastPathSegment();
        } else { // web URI
            return intent.getData().getQueryParameter(altParamId);
        }
    }

    public static String wrapHtml(Context context, String html) {
        return context.getString(R.string.html,
                Preferences.Theme.getReadabilityTypeface(context),
                toHtmlPx(context, Preferences.Theme.resolvePreferredReadabilityTextSize(context)),
                AppUtils.toHtmlColor(context, android.R.attr.textColorPrimary),
                AppUtils.toHtmlColor(context, android.R.attr.textColorLink),
                TextUtils.isEmpty(html) ? context.getString(R.string.empty_text) : html,
                toHtmlPx(context, context.getResources().getDimension(R.dimen.activity_vertical_margin)),
                toHtmlPx(context, context.getResources().getDimension(R.dimen.activity_horizontal_margin)),
                Preferences.getReadabilityLineHeight(context));
    }

    private static float toHtmlPx(Context context, @StyleRes int textStyleAttr) {
        return toHtmlPx(context, AppUtils.getDimension(context, textStyleAttr, R.attr.contentTextSize));
    }

    private static float toHtmlPx(Context context, float dimen) {
        return dimen / context.getResources().getDisplayMetrics().density;
    }

    private static CharSequence trim(CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            return charSequence;
        }
        int end = charSequence.length() - 1;
        while (Character.isWhitespace(charSequence.charAt(end))) {
            end--;
        }
        return charSequence.subSequence(0, end + 1);
    }

    @NonNull
    private static Intent createViewIntent(Context context, @Nullable WebItem item,
                                           String url, @Nullable CustomTabsSession session) {
        if (Preferences.customTabsEnabled(context)) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(session)
                    .setToolbarColor(ContextCompat.getColor(context,
                            AppUtils.getThemedResId(context, R.attr.colorPrimary)))
                    .setShowTitle(true)
                    .enableUrlBarHiding()
                    .addDefaultShareMenuItem();
            if (item != null) {
                builder.addMenuItem(context.getString(R.string.comments),
                        PendingIntent.getActivity(context, 0,
                                new Intent(context, ItemActivity.class)
                                        .putExtra(ItemActivity.EXTRA_ITEM, item)
                                        .putExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true),
                                PendingIntent.FLAG_ONE_SHOT));
            }
            return builder.build().intent.setData(Uri.parse(url));
        } else {
            return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        }
    }

    @SuppressLint("InlinedApi")
    public static Intent multiWindowIntent(Activity activity, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }
        return intent;
    }

    public static void setTextAppearance(TextView textView, @StyleRes int textAppearance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setTextAppearance(textAppearance);
        } else {
            //noinspection deprecation
            textView.setTextAppearance(textView.getContext(), textAppearance);
        }
    }

    static class SystemUiHelper {
        private final Window window;
        private final int originalUiFlags;
        private boolean enabled = true;

        SystemUiHelper(Window window) {
            this.window = window;
            this.originalUiFlags = window.getDecorView().getSystemUiVisibility();
        }

        @SuppressLint("InlinedApi")
        void setFullscreen(boolean fullscreen) {
            if (!enabled) {
                return;
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                return;
            }
            if (fullscreen) {
                window.getDecorView().setSystemUiVisibility(originalUiFlags |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                window.getDecorView().setSystemUiVisibility(originalUiFlags);
            }
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
