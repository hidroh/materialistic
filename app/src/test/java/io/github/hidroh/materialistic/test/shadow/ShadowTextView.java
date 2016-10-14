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

package io.github.hidroh.materialistic.test.shadow;

import android.graphics.Typeface;
import android.widget.TextView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(TextView.class)
public class ShadowTextView extends org.robolectric.shadows.ShadowTextView {
    private Typeface typeface;
    private int lineCount = 0;

    @Implementation
    public void setTypeface(Typeface tf) {
        typeface = tf;
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }
    @Implementation
    public int getLineCount() {
        return lineCount;
    }
}
