package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DimenRes;
import android.support.v4.app.BundleCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;

public class AppUtils {
    //  Must have. Extra used to match the session. Its value is an IBinder passed
    //  whilst creating a news session. See newSession() below. Even if the service is not
    //  used and there is no valid session id to be provided, this extra has to be present
    //  with a null value to launch a custom tab.
    private static final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";

    // Extra that changes the background color for the omnibox. colorInt is an int
    // that specifies a Color.
    private static final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

    public static void openWebUrl(Context context, ItemManager.WebItem item) {
        final boolean isExternal = Preferences.externalBrowserEnabled(context);
        if (isExternal) {
            openWebUrlExternal(context, item.getUrl());
        } else {
            final Intent intent = new Intent(context, WebActivity.class);
            intent.putExtra(WebActivity.EXTRA_ITEM, item);
            context.startActivity(intent);
        }
    }

    public static void openWebUrlExternal(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        Bundle extras = new Bundle();
        BundleCompat.putBinder(extras, EXTRA_CUSTOM_TABS_SESSION, null);
        intent.putExtras(extras);
        intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR,
                ContextCompat.getColor(context, R.color.colorPrimary));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    public static void setTextWithLinks(TextView textView, String htmlText) {
        setHtmlText(textView, htmlText);
        textView.setOnTouchListener(new View.OnTouchListener() {
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
        textView.setText(TextUtils.isEmpty(htmlText) ? null : Html.fromHtml(htmlText));
    }

    public static Intent makeEmailIntent(String subject, String text) {
        final Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return intent;
    }

    public static Intent makeShareIntent(String text) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return intent;
    }

    public static int getThemedResId(Context context, int attr) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        final int resId = a.getResourceId(0, 0);
        a.recycle();
        return resId;
    }

    public static boolean isHackerNewsUrl(ItemManager.WebItem item) {
        return !TextUtils.isEmpty(item.getUrl()) &&
                item.getUrl().equals(String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()));
    }

    public static int getDimensionInDp(Context context, @DimenRes int dimenResId) {
        return (int) (context.getResources().getDimension(dimenResId) /
                        context.getResources().getDisplayMetrics().density);
    }
}
