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

package io.github.hidroh.materialistic;

import androidx.core.widget.NestedScrollView;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import io.github.hidroh.materialistic.test.TestRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(TestRunner.class)
public class NestedScrollViewScrollHelperTest {
    private NestedScrollView scrollView;
    private KeyDelegate.NestedScrollViewHelper helper;

    @Before
    public void setUp() {
        scrollView = mock(NestedScrollView.class);
        helper = new KeyDelegate.NestedScrollViewHelper(scrollView);
    }

    @Test
    public void testScrollToTop() {
        helper.scrollToTop();
        verify(scrollView).smoothScrollTo(eq(0), eq(0));
    }

    @Test
    public void testScrollToNext() {
        helper.scrollToNext();
        verify(scrollView).pageScroll(eq(View.FOCUS_DOWN));
    }

    @Test
    public void testScrollToPrevious() {
        helper.scrollToPrevious();
        verify(scrollView).pageScroll(View.FOCUS_UP);
    }
}
