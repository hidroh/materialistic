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

package io.github.hidroh.materialistic;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.FeedbackClient;

public abstract class DrawerActivity extends InjectableActivity {

    @Inject FeedbackClient mFeedbackClient;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private View mDrawer;
    private Class<? extends Activity> mPendingNavigation;
    private Bundle mPendingNavigationExtras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_drawer);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawer = findViewById(R.id.drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open_drawer,
                R.string.close_drawer) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (drawerView.equals(mDrawer) && mPendingNavigation != null) {
                    final Intent intent = new Intent(DrawerActivity.this, mPendingNavigation);
                    if (mPendingNavigationExtras != null) {
                        intent.putExtras(mPendingNavigationExtras);
                        mPendingNavigationExtras = null;
                    }
                    // TODO M bug https://code.google.com/p/android/issues/detail?id=193822
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    mPendingNavigation = null;
                }
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item)|| super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mDrawer)) {
            closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup drawerLayout = (ViewGroup) findViewById(R.id.drawer_layout);
        View view = getLayoutInflater().inflate(layoutResID, drawerLayout, false);
        drawerLayout.addView(view, 0);
    }

    void navigate(Class<? extends Activity> activityClass, @Nullable Bundle extras) {
        mPendingNavigation = !getClass().equals(activityClass) ? activityClass : null;
        mPendingNavigationExtras = extras;
        closeDrawers();
    }

    void showFeedback() {
        showFeedbackDialog(getLayoutInflater().inflate(R.layout.dialog_feedback, null, false));
        closeDrawers();
    }

    private void closeDrawers() {
        mDrawerLayout.closeDrawers();
    }

    private void showFeedbackDialog(View dialogView) {
        AppUtils.setTextWithLinks((TextView) dialogView.findViewById(R.id.feedback_note),
                getString(R.string.feedback_note));
        final TextInputLayout titleLayout = (TextInputLayout)
                dialogView.findViewById(R.id.textinput_title);
        final TextInputLayout bodyLayout = (TextInputLayout)
                dialogView.findViewById(R.id.textinput_body);
        final EditText title = (EditText) dialogView.findViewById(R.id.edittext_title);
        final EditText body = (EditText) dialogView.findViewById(R.id.edittext_body);
        final View sendButton = dialogView.findViewById(R.id.feedback_button);
        final Dialog dialog = mAlertDialogBuilder
                .init(this)
                .setView(dialogView)
                .create();
        dialogView.findViewById(R.id.button_rate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtils.openPlayStore(DrawerActivity.this);
                dialog.dismiss();
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                titleLayout.setErrorEnabled(false);
                bodyLayout.setErrorEnabled(false);
                if (title.length() == 0) {
                    titleLayout.setError(getString(R.string.title_required));
                }
                if (body.length() == 0) {
                    bodyLayout.setError(getString(R.string.comment_required));
                }
                if (title.length() == 0 || body.length() == 0) {
                    return;
                }
                sendButton.setEnabled(false);
                mFeedbackClient.send(title.getText().toString(), body.getText().toString(),
                        new FeedbackClient.Callback() {
                            @Override
                            public void onSent(boolean success) {
                                Toast.makeText(DrawerActivity.this,
                                        success ? R.string.feedback_sent : R.string.feedback_failed,
                                        Toast.LENGTH_SHORT)
                                        .show();
                                if (!dialog.isShowing()) {
                                    return;
                                }
                                if (success) {
                                    dialog.dismiss();
                                } else {
                                    sendButton.setEnabled(true);
                                }
                            }
                        });
            }
        });
        dialog.show();
    }
}
