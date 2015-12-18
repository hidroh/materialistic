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

package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.text.style.ReplacementSpan;

import io.github.hidroh.materialistic.R;

public class AsteriskSpan extends ReplacementSpan {
    private final int mBackgroundColor;
    private final int mTextColor;
    private final float mPadding;

    public AsteriskSpan(Context context) {
        super();
        mBackgroundColor = ContextCompat.getColor(context, R.color.redA200);
        mTextColor = ContextCompat.getColor(context, R.color.white);
        mPadding = context.getResources().getDimension(R.dimen.padding_asterisk);
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end) + mPadding * 4);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        float textSize = paint.measureText(text, start, end);
        float radius = textSize / 2 + mPadding;
        float centerX = x + radius + mPadding;
        float centerY = y / 2;
        paint.setColor(mBackgroundColor);
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.setColor(mTextColor);
        canvas.drawText(text, start, end, x + mPadding * 2, y, paint);
    }
}
