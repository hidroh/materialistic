package io.github.hidroh.materialistic;

import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.TypefaceSpan;

import org.xml.sax.XMLReader;

/**
 * TagHandler that 'knows' how to handle <code>code<code> tags.
 * This handler can be used in the Html.fromHtml method to support formatting of code tags using the monospace typeface.
 *
 * Hackernews returns <code>&lt;pre&gt;&lt;code&gt;</code> blocks, but we are only interested in the inner code.
 * The pre tag is then ignored by the Html.fromHtml default tag handler.
 */
public class HtmlCodeTagHandler implements Html.TagHandler {
    @Override
    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
        if (tag.equalsIgnoreCase("code")) {
            if (opening) {
                start(output, new Monospace());
            } else {
                end(output, Monospace.class, new TypefaceSpan("monospace"));
            }
        }
    }

    // This is copied from android.text.HtmlToSpannedConverter#start
    private static void start(Editable text, Object mark) {
        int len = text.length();
        text.setSpan(mark, len, len, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    // This is copied from android.text.HtmlToSpannedConverter#end
    private static void end(Editable text, Class kind, Object repl) {
        Object obj = getLast(text, kind);
        if (obj != null) {
            setSpanFromMark(text, obj, repl);
        }
    }

    // This is copied from android.text.HtmlToSpannedConverter#setSpanFromMark
    private static void setSpanFromMark(Spannable text, Object mark, Object... spans) {
        int where = text.getSpanStart(mark);
        text.removeSpan(mark);
        int len = text.length();
        if (where != len) {
            for (Object span : spans) {
                text.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    // This is copied from android.text.HtmlToSpannedConverter#getLast
    private static <T> T getLast(Spanned text, Class<T> kind) {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
        T[] objs = text.getSpans(0, text.length(), kind);
        if (objs.length == 0) {
            return null;
        } else {
            return objs[objs.length - 1];
        }
    }
    // This is copied from android.text.HtmlToSpannedConverter.Monospace
    private static class Monospace {}
}
