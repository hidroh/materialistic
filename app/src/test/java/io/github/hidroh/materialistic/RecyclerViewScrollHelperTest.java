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

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import io.github.hidroh.materialistic.test.TestRunner;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(TestRunner.class)
public class RecyclerViewScrollHelperTest {
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private KeyDelegate.RecyclerViewHelper helper;
    private RecyclerView.Adapter adapter;

    @Before
    public void setUp() {
        layoutManager = mock(LinearLayoutManager.class);
        adapter = mock(RecyclerView.Adapter.class);
        recyclerView = mock(RecyclerView.class);
        when(recyclerView.getLayoutManager()).thenReturn(layoutManager);
        when(recyclerView.getAdapter()).thenReturn(adapter);
    }

    @Test
    public void testScrollToTop() {
        helper = new KeyDelegate.RecyclerViewHelper(recyclerView,
                KeyDelegate.RecyclerViewHelper.SCROLL_ITEM);
        helper.scrollToTop();
        verify(recyclerView).scrollToPosition(eq(0));
    }

    @Test
    public void testScrollToNextItem() {
        when(adapter.getItemCount()).thenReturn(10);
        when(layoutManager.findFirstVisibleItemPosition()).thenReturn(0);
        helper = new KeyDelegate.RecyclerViewHelper(recyclerView,
                KeyDelegate.RecyclerViewHelper.SCROLL_ITEM);
        assertTrue(helper.scrollToNext());
        verify(recyclerView).smoothScrollToPosition(eq(1));

        when(layoutManager.findFirstVisibleItemPosition()).thenReturn(RecyclerView.NO_POSITION);
        assertFalse(helper.scrollToNext());
    }

    @Test
    public void testScrollToNextPage() {
        when(adapter.getItemCount()).thenReturn(10);
        when(layoutManager.findLastCompletelyVisibleItemPosition()).thenReturn(8);
        helper = new KeyDelegate.RecyclerViewHelper(recyclerView,
                KeyDelegate.RecyclerViewHelper.SCROLL_PAGE);
        assertTrue(helper.scrollToNext());
        verify(recyclerView).smoothScrollToPosition(eq(9));

        when(layoutManager.findLastCompletelyVisibleItemPosition()).thenReturn(9);
        assertFalse(helper.scrollToNext());
    }

    @Test
    public void testScrollToPreviousItem() {
        when(adapter.getItemCount()).thenReturn(10);
        when(layoutManager.findFirstVisibleItemPosition()).thenReturn(5);
        helper = new KeyDelegate.RecyclerViewHelper(recyclerView,
                KeyDelegate.RecyclerViewHelper.SCROLL_ITEM);
        assertTrue(helper.scrollToPrevious());
        verify(recyclerView).smoothScrollToPosition(eq(4));

        when(layoutManager.findFirstVisibleItemPosition()).thenReturn(0);
        assertFalse(helper.scrollToPrevious());

        when(layoutManager.findFirstVisibleItemPosition()).thenReturn(RecyclerView.NO_POSITION);
        assertFalse(helper.scrollToPrevious());
    }

    @Test
    public void testScrollToPreviousPage() {
        when(layoutManager.findFirstVisibleItemPosition()).thenReturn(5);
        helper = new KeyDelegate.RecyclerViewHelper(recyclerView,
                KeyDelegate.RecyclerViewHelper.SCROLL_PAGE);
        assertTrue(helper.scrollToPrevious());
        verify(recyclerView).smoothScrollBy(anyInt(), anyInt());

        when(layoutManager.findFirstVisibleItemPosition()).thenReturn(0);
        assertFalse(helper.scrollToPrevious());
    }

    @After
    public void tearDown() {

    }
}
