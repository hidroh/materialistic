package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import io.github.hidroh.materialistic.data.HackerNewsClient;

public class AppUtils {
    public static void openWebUrl(Context context, HackerNewsClient.WebItem item) {
        final boolean isExternal = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_external), false);
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
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    /**
     * Tint drawable with given color for pre-Lollipop, should be called only once after the drawable
     * has been inflated for the first time
     * @param res           Android resources
     * @param drawableResId drawable to tint
     * @param colorResId    color to apply
     */
    public static void initTintedDrawable(Resources res,
                                          @DrawableRes int drawableResId, @ColorRes int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return; // use tint attribute instead
        }

        res.getDrawable(drawableResId).setColorFilter(res.getColor(colorResId), PorterDuff.Mode.SRC_IN);
    }

    public static void setTextWithLinks(TextView textView, String htmlText) {
        setHtmlText(textView, htmlText);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
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
}
