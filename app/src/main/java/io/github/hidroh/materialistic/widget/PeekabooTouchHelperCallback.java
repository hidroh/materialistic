/*
 * Copyright (c) 2016 Ha Duy Trung
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

package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.View;

import java.util.Locale;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.R;

abstract class PeekabooTouchHelperCallback extends ItemTouchHelper.SimpleCallback {
    private final Paint mPaint = new Paint();
    private final int mPadding;
    private final int mDefaultTextColor;

    PeekabooTouchHelperCallback(Context context) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        mDefaultTextColor = ContextCompat.getColor(context,
                AppUtils.getThemedResId(context, android.R.attr.textColorPrimary));
        mPaint.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.text_size_small));
        mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mPadding = context.getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        drawPeekingText(c, viewHolder, dX);
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawPeekingText(Canvas canvas, RecyclerView.ViewHolder viewHolder, float dX) {
        View itemView = viewHolder.itemView;
        boolean swipeRight = dX > 0;
        String text = swipeRight ? getRightText() : getLeftText();
        Rect rect = new Rect();
        mPaint.getTextBounds(text, 0, text.length(), rect);
        float textWidth = rect.right - rect.left,
                textHeight = rect.bottom - rect.top,
                width = itemView.getWidth(),
                paddingY = (itemView.getHeight() - textHeight) / 2;
        mPaint.setColor(swipeRight ? getRightTextColor() : getLeftTextColor());
        mPaint.setAlpha(Math.min(255, (int) (255 / getSwipeThreshold(viewHolder) * Math.abs(dX) / width)));
        canvas.drawText(text.toUpperCase(Locale.getDefault()),
                swipeRight ? itemView.getLeft() + mPadding :
                        itemView.getRight() - mPadding - textWidth,
                itemView.getBottom() - paddingY,
                mPaint);
    }

    @Override
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        return 1f/3;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return Float.MAX_VALUE;
    }

    protected abstract String getLeftText();

    protected abstract String getRightText();

    protected int getLeftTextColor() {
        return mDefaultTextColor;
    }

    protected int getRightTextColor() {
        return mDefaultTextColor;
    }
}
