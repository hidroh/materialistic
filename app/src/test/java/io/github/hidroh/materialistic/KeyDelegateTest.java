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

import android.app.Activity;
import android.preference.PreferenceManager;
import com.google.android.material.appbar.AppBarLayout;
import android.view.KeyEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import io.github.hidroh.materialistic.test.TestRunner;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(TestRunner.class)
public class KeyDelegateTest {
    private KeyDelegate delegate;
    private ActivityController<Activity> controller;
    private Activity activity;
    private Scrollable scrollable = mock(Scrollable.class);
    private AppBarLayout appBar = mock(AppBarLayout.class);

    @Before
    public void setUp() {
        reset(scrollable, appBar);
        controller = Robolectric.buildActivity(Activity.class);
        activity = controller.create().get();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_volume), true)
                .apply();
        delegate = new KeyDelegate();
        delegate.attach(activity);
        delegate.setScrollable(scrollable, appBar);
    }

    @Test
    public void testInterceptBack() {
        assertFalse(delegate.onKeyDown(KeyEvent.KEYCODE_BACK,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)));
        delegate.setBackInterceptor(() -> true);
        assertTrue(delegate.onKeyDown(KeyEvent.KEYCODE_BACK,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)));
        delegate.setBackInterceptor(() -> false);
        assertFalse(delegate.onKeyDown(KeyEvent.KEYCODE_BACK,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)));
    }

    @Test
    public void testOnKeyDown() {
        assertTrue(delegate.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)));
        assertTrue(delegate.onKeyDown(KeyEvent.KEYCODE_VOLUME_DOWN,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)));
        assertFalse(delegate.onKeyDown(KeyEvent.KEYCODE_BACK,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)));
    }

    @Test
    public void testOnKeyUp() {
        KeyEvent keyEvent = mock(KeyEvent.class);
        when(keyEvent.getFlags()).thenReturn(KeyEvent.FLAG_CANCELED_LONG_PRESS);
        assertFalse(delegate.onKeyUp(KeyEvent.KEYCODE_BACK, keyEvent));
        assertFalse(delegate.onKeyUp(KeyEvent.KEYCODE_VOLUME_UP, keyEvent));
        assertFalse(delegate.onKeyUp(KeyEvent.KEYCODE_VOLUME_DOWN, keyEvent));

        KeyEvent keyEventVolUp = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP),
                keyEventVolDown = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN);
        assertTrue(delegate.onKeyUp(KeyEvent.KEYCODE_VOLUME_UP, keyEventVolUp));
        verify(scrollable).scrollToPrevious();
        verify(appBar).setExpanded(eq(true), anyBoolean());

        reset(scrollable, appBar);
        when(scrollable.scrollToPrevious()).thenReturn(true);
        assertTrue(delegate.onKeyUp(KeyEvent.KEYCODE_VOLUME_UP, keyEventVolUp));
        verify(scrollable).scrollToPrevious();
        verify(appBar, never()).setExpanded(eq(true), anyBoolean());

        reset(scrollable, appBar);
        when(appBar.getHeight()).thenReturn(10);
        when(appBar.getBottom()).thenReturn(10);
        assertTrue(delegate.onKeyUp(KeyEvent.KEYCODE_VOLUME_DOWN, keyEventVolDown));
        verify(scrollable, never()).scrollToNext();
        verify(appBar).setExpanded(eq(false), anyBoolean());

        reset(scrollable, appBar);
        when(appBar.getHeight()).thenReturn(10);
        when(appBar.getBottom()).thenReturn(0);
        assertTrue(delegate.onKeyUp(KeyEvent.KEYCODE_VOLUME_DOWN, keyEventVolDown));
        verify(scrollable).scrollToNext();
        verify(appBar, never()).setExpanded(eq(false), anyBoolean());
    }

    @Test
    public void testOnKeyLongPress() {
        assertFalse(delegate.onKeyLongPress(KeyEvent.KEYCODE_BACK,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)));

        assertTrue(delegate.onKeyLongPress(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP)));
        verify(appBar).setExpanded(eq(true), anyBoolean());
        verify(scrollable).scrollToTop();

        assertTrue(delegate.onKeyLongPress(KeyEvent.KEYCODE_VOLUME_DOWN,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN)));
    }

    @Test
    public void testNoBinding() {
        delegate.setScrollable(null, null);

        assertTrue(delegate.onKeyUp(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP)));
        assertTrue(delegate.onKeyUp(KeyEvent.KEYCODE_VOLUME_DOWN,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN)));

        assertTrue(delegate.onKeyLongPress(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP)));
        assertTrue(delegate.onKeyLongPress(KeyEvent.KEYCODE_VOLUME_DOWN,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN)));
    }

    @Test
    public void testOnKeyDownDisabled() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_volume), false)
                .apply();
        assertFalse(delegate.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)));
    }

    @Test
    public void testOnKeyUpDisabled() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_volume), false)
                .apply();
        assertFalse(delegate.onKeyUp(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP)));
    }

    @Test
    public void testOnKeyLongPressDisabled() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_volume), false)
                .apply();
        assertFalse(delegate.onKeyLongPress(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP)));
    }

    @After
    public void tearDown() {
        delegate.detach(activity);
        controller.destroy();
    }
}
