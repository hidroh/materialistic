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
import android.accounts.OnAccountsUpdateListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.AttrRes;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.IntentCompat;
import android.support.v4.util.Pair;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.WebItem;

public class AppUtils {
    private static final String ABBR_YEAR = "y";
    private static final String ABBR_WEEK = "w";
    private static final String ABBR_DAY = "d";
    private static final String ABBR_HOUR = "h";
    private static final String ABBR_MINUTE = "m";
    private static final String PLAY_STORE_URL = "market://details?id=" + BuildConfig.APPLICATION_ID;

    public static void openWebUrlExternal(Context context, String title, String url) {
        Intent intent = createViewIntent(context, title, url);
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
            intents.add(createViewIntent(context, title, url)
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

    public static void setTextWithLinks(TextView textView, String htmlText) {
        setHtmlText(textView, htmlText);
        // TODO https://code.google.com/p/android/issues/detail?id=191430
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

                    ClickableSpan[] link = Spannable.Factory.getInstance()
                            .newSpannable(widget.getText())
                            .getSpans(off, off, ClickableSpan.class);

                    if (link.length != 0) {
                        if (action == MotionEvent.ACTION_UP) {
                            link[0].onClick(widget);
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public static void setHtmlText(TextView textView, String htmlText) {
        textView.setText(TextUtils.isEmpty(htmlText) ? null : trim(Html.fromHtml(htmlText)));
    }

    public static Intent makeEmailIntent(String subject, String text) {
        final Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return intent;
    }

    public static void openExternal(@NonNull final Context context,
                                    @NonNull AlertDialogBuilder alertDialogBuilder,
                                    @NonNull final WebItem item) {
        if (TextUtils.isEmpty(item.getUrl()) ||
                item.getUrl().startsWith(HackerNewsClient.BASE_WEB_URL)) {
            openWebUrlExternal(context,
                    item.getDisplayedTitle(),
                    String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()));
            return;
        }
        alertDialogBuilder
                .init(context)
                .setMessage(R.string.view_in_browser)
                .setPositiveButton(R.string.article, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openWebUrlExternal(context,
                                item.getDisplayedTitle(),
                                item.getUrl());
                    }
                })
                .setNegativeButton(R.string.comments, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openWebUrlExternal(context,
                                item.getDisplayedTitle(),
                                String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()));
                    }
                })
                .create()
                .show();

    }

    public static void share(@NonNull final Context context,
                             @NonNull AlertDialogBuilder alertDialogBuilder,
                             @NonNull final WebItem item) {
        if (TextUtils.isEmpty(item.getUrl()) ||
                item.getUrl().startsWith(HackerNewsClient.BASE_WEB_URL)) {
            context.startActivity(makeChooserShareIntent(context,
                    item.getDisplayedTitle(),
                    String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId())));
            return;
        }
        alertDialogBuilder
                .init(context)
                .setMessage(R.string.share)
                .setPositiveButton(R.string.article, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        context.startActivity(makeChooserShareIntent(context,
                                item.getDisplayedTitle(),
                                item.getUrl()));
                    }
                })
                .setNegativeButton(R.string.comments, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        context.startActivity(makeChooserShareIntent(context,
                                item.getDisplayedTitle(),
                                String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId())));
                    }
                })
                .create()
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

    public static void restart(Activity activity) {
        activity.finish();
        final Intent intent = activity.getIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
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

    public static void registerAccountsUpdatedListener(final Context context) {
        AccountManager.get(context).addOnAccountsUpdatedListener(new OnAccountsUpdateListener() {
            @Override
            public void onAccountsUpdated(Account[] accounts) {
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
            }
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

    public static void showAccountChooser(final Context context, AlertDialogBuilder alertDialogBuilder,
                                           Account[] accounts) {
        String username = Preferences.getUsername(context);
        final String[] items = new String[accounts.length + 1];
        int checked = -1;
        for (int i = 0; i < accounts.length; i++) {
            String accountName = accounts[i].name;
            items[i] = accountName;
            if (TextUtils.equals(accountName, username)) {
                checked = i;
            }
        }
        items[items.length - 1] = context.getString(R.string.add_account);
        alertDialogBuilder
                .init(context)
                .setTitle(R.string.choose_account)
                .setSingleChoiceItems(items, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == items.length - 1) {
                            Intent intent = new Intent(context, LoginActivity.class);
                            intent.putExtra(LoginActivity.EXTRA_ADD_ACCOUNT, true);
                            context.startActivity(intent);
                        } else {
                            Preferences.setUsername(context, items[which]);
                            Toast.makeText(context,
                                    context.getString(R.string.welcome, items[which]),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public static void toggleFab(FloatingActionButton fab, boolean visible) {
        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
        if (visible) {
            fab.show();
            p.setBehavior(new ScrollAwareFABBehavior());
        } else {
            fab.hide();
            p.setBehavior(null);
        }
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

    private static Intent makeShareIntent(String subject, String text) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return intent;
    }

    private static Intent makeChooserShareIntent(Context context, String subject, String text) {
        Intent shareIntent = AppUtils.makeShareIntent(subject, text);
        Intent chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.share));
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return chooserIntent;
    }

    @NonNull
    private static Intent createViewIntent(Context context, String title, String url) {
        if (Preferences.customChromeTabEnabled(context)) {
            Intent shareIntent = new Intent(context, ShareBroadcastReceiver.class);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                    .setToolbarColor(ContextCompat.getColor(context, R.color.orange500))
                    .setActionButton(BitmapFactory.decodeResource(context.getResources(),
                                    R.drawable.ic_share_grey600_24dp),
                            context.getString(R.string.share),
                            PendingIntent.getBroadcast(context, 0, shareIntent,
                                    PendingIntent.FLAG_ONE_SHOT))
                    .build();
            customTabsIntent.intent.setData(Uri.parse(url));
            return customTabsIntent.intent;
        } else {
            return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        }
    }

    public static class ShareBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            context.startActivity(makeChooserShareIntent(context,
                    intent.getStringExtra(Intent.EXTRA_SUBJECT),
                    intent.getStringExtra(Intent.EXTRA_TEXT)));
        }
    }
}
