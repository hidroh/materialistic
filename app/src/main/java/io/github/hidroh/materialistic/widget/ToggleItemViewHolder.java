package io.github.hidroh.materialistic.widget;

import android.view.View;
import android.widget.TextView;

import io.github.hidroh.materialistic.R;

public class ToggleItemViewHolder extends ItemRecyclerViewAdapter.ItemViewHolder {
    public final TextView mToggle;

    public ToggleItemViewHolder(View itemView) {
        super(itemView);
        mToggle = (TextView) itemView.findViewById(R.id.toggle);
    }
}
