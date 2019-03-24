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

import android.app.Dialog;
import android.content.Intent;
import androidx.appcompat.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import io.github.hidroh.materialistic.test.TestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowGestureDetector;

import io.github.hidroh.materialistic.Navigable;
import io.github.hidroh.materialistic.R;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunner.class)
public class NavFloatingActionButtonTest {
    private NavFloatingActionButton button;
    private Navigable navigable;
    private GestureDetector.SimpleOnGestureListener gestureListener;

    @Before
    public void setUp() {
        button = new NavFloatingActionButton(new ContextThemeWrapper(RuntimeEnvironment.application,
                R.style.AppTheme));
        shadowOf(button).getOnTouchListener().onTouch(button,
                MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0));
        gestureListener = (GestureDetector.SimpleOnGestureListener)
                shadowOf(ShadowGestureDetector.getLastActiveDetector()).getListener();
        navigable = mock(Navigable.class);
        button.setNavigable(navigable);
        assertTrue(gestureListener.onDown(null));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNoTouchOverride() {
        button.setOnTouchListener(mock(View.OnTouchListener.class));
    }

    @Test
    public void testSwipeUp() {
        gestureListener.onFling(null, null, 0f, -1f); // up
        verify(navigable).onNavigate(eq(Navigable.DIRECTION_UP));
    }

    @Test
    public void testSwipeDown() {
        gestureListener.onFling(null, null, 0f, 1f); // down
        verify(navigable).onNavigate(eq(Navigable.DIRECTION_DOWN));
    }

    @Test
    public void testSwipeLeft() {
        gestureListener.onFling(null, null, -1f, 0f); // left
        verify(navigable).onNavigate(eq(Navigable.DIRECTION_LEFT));
    }

    @Test
    public void testSwipeRight() {
        gestureListener.onFling(null, null, 1f, 0f); // right
        verify(navigable).onNavigate(eq(Navigable.DIRECTION_RIGHT));
    }

    @Test
    public void testKonami() {
        gestureListener.onFling(null, null, 0f, 1f); // down, invalid, should ignore

        gestureListener.onFling(null, null, 0f, -1f); // up
        gestureListener.onFling(null, null, 0f, 1f); // down, invalid, should reset

        gestureListener.onFling(null, null, 0f, -1f); // up
        gestureListener.onFling(null, null, 0f, -1f); // up
        gestureListener.onFling(null, null, 0f, 1f); // down
        gestureListener.onFling(null, null, 0f, 1f); // down
        gestureListener.onFling(null, null, -1f, 0f); // left
        gestureListener.onFling(null, null, 1f, 0f); // right
        gestureListener.onFling(null, null, -1f, 0f); // left
        gestureListener.onFling(null, null, 1f, 0f); // right
        gestureListener.onDoubleTap(null);
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        shadowOf(dialog).clickOn(android.R.id.button1); // BUTTON_POSITIVE
        assertThat(shadowOf(RuntimeEnvironment.application).getNextStartedActivity())
                .hasAction(Intent.ACTION_VIEW);
    }
}
