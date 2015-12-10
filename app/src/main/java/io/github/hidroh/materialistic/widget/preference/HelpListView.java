package io.github.hidroh.materialistic.widget.preference;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.widget.AsteriskSpan;

public class HelpListView extends ScrollView {
    public HelpListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        addView(LayoutInflater.from(context).inflate(R.layout.include_help_list_view, this, false));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ((TextView) findViewById(R.id.item_new).findViewById(R.id.rank))
                .append(makeAsteriskSpan());
        ((TextView) findViewById(R.id.item_promoted).findViewById(R.id.rank))
                .setTextColor(ContextCompat.getColor(getContext(), R.color.greenA700));
        Button comments = (Button) findViewById(R.id.item_new_comments).findViewById(R.id.comment);
        SpannableStringBuilder sb = new SpannableStringBuilder(comments.getText());
        sb.append(makeAsteriskSpan());
        comments.setText(sb);
    }

    private Spannable makeAsteriskSpan() {
        SpannableString sb = new SpannableString("*");
        sb.setSpan(new AsteriskSpan(getContext()), sb.length() - 1, sb.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }
}
