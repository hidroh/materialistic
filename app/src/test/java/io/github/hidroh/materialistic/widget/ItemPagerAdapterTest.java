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
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.WebFragment;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.test.ShadowWebView;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = ShadowWebView.class)
@RunWith(RobolectricGradleTestRunner.class)
public class ItemPagerAdapterTest {
    private ActivityController<TestActivity> controller;
    private TestActivity activity;
    private ItemPagerAdapter adapter;
    private WebFragment fragment;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestActivity.class);
        activity = controller.create().start().resume().visible().get();
        adapter = new ItemPagerAdapter(activity, activity.getSupportFragmentManager(),
                new TestHnItem(1L) {
                    @Override
                    public String getUrl() {
                        return "http://example.com";
                    }
                }, true, ItemManager.MODE_DEFAULT);
        fragment = (WebFragment) adapter.instantiateItem((ViewGroup) activity.findViewById(android.R.id.content), 1);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment, WebFragment.class.getName())
                .commit();
    }

    @Test
    public void testFabClick() {
        FloatingActionButton fab = new FloatingActionButton(activity);
        adapter.onFabClick(fab, 1);
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertThat(fab).isNotVisible();
        dialog.dismiss();
        assertThat(fab).isVisible();
    }

    @Test
    public void testSearch() {
        adapter.onFabClick(new FloatingActionButton(activity), 1);
        Dialog dialog = ShadowDialog.getLatestDialog();
        View buttonClear = dialog.findViewById(R.id.button_clear);
        EditText editText = (EditText) dialog.findViewById(R.id.edittext);
        assertThat(buttonClear).isNotVisible();
        editText.setText("abc");
        assertThat(buttonClear).isVisible();
        ShadowWebView shadowWebView = (ShadowWebView) ShadowExtractor.extract(fragment.getWebView());
        // no matches
        shadowWebView.setFindCount(0);
        shadowOf(editText).getOnEditorActionListener().onEditorAction(null, 0, null);
        assertThat(ShadowToast.getTextOfLatestToast()).contains(activity.getString(R.string.no_matches));
        assertFalse(shadowOf(dialog).hasBeenDismissed());

        // has matches
        shadowWebView.setFindCount(1);
        shadowOf(editText).getOnEditorActionListener().onEditorAction(null, 0, null);
        assertTrue(shadowOf(dialog).hasBeenDismissed());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    static class TestActivity extends AppCompatActivity {}
}
