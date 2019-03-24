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
import androidx.cardview.widget.CardView;
import android.util.AttributeSet;

public class FlatCardView extends CardView {

    public FlatCardView(Context context) {
        super(context);
    }

    public FlatCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlatCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void flatten() {
        setRadius(0);
        setUseCompatPadding(false);
        if (getLayoutParams() instanceof MarginLayoutParams) {
            MarginLayoutParams marginLayoutParams = (MarginLayoutParams) getLayoutParams();
            marginLayoutParams.leftMargin = getContentPaddingLeft() - getPaddingLeft();
            marginLayoutParams.rightMargin = getContentPaddingRight() - getPaddingRight();
            marginLayoutParams.topMargin = getContentPaddingTop() - getPaddingTop();
            marginLayoutParams.bottomMargin = getContentPaddingBottom() - getPaddingBottom();
        }
    }
}
