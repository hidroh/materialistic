package io.github.hidroh.materialistic.widget;

import android.view.View;
import android.widget.TextView;

import io.github.hidroh.materialistic.R;

/**
 * Created by hadt on 18/12/15.
 */
public class SubmissionViewHolder extends ItemRecyclerViewAdapter.ItemViewHolder {
    final TextView mTitleTextView;
    final TextView mParentTextView;

    public SubmissionViewHolder(View itemView) {
        super(itemView);
        mTitleTextView = (android.widget.TextView) itemView.findViewById(R.id.title);
        mParentTextView = (android.widget.TextView) itemView.findViewById(R.id.parent);
        mCommentButton.setText(R.string.view);
    }
}
