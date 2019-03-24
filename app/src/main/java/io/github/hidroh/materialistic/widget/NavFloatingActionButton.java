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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Vibrator;
import androidx.annotation.RequiresApi;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.view.GestureDetectorCompat;
import androidx.appcompat.app.AlertDialog;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Locale;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.Navigable;
import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.annotation.Synthetic;

public class NavFloatingActionButton extends FloatingActionButton implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String PREFERENCES_FAB = "_fab";
    private static final String PREFERENCES_FAB_X = "%1$s_%2$d_%3$d_x";
    private static final String PREFERENCES_FAB_Y = "%1$s_%2$d_%3$d_y";
    private static final long VIBRATE_DURATION_MS = 15;
    private static final int DOUBLE_TAP = -1;
    private static final int[] KONAMI_CODE = {
            Navigable.DIRECTION_UP,
            Navigable.DIRECTION_UP,
            Navigable.DIRECTION_DOWN,
            Navigable.DIRECTION_DOWN,
            Navigable.DIRECTION_LEFT,
            Navigable.DIRECTION_RIGHT,
            Navigable.DIRECTION_LEFT,
            Navigable.DIRECTION_RIGHT,
            DOUBLE_TAP
    };
    @Synthetic final Vibrator mVibrator;
    private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();
    @Synthetic Navigable mNavigable;
    @Synthetic boolean mMoved;
    private int mNextKonamiCode = 0;
    private SharedPreferences mPreferences;
    private String mPreferenceX, mPreferenceY;
    @Synthetic boolean mVibrationEnabled;

    public static void resetPosition(Context context) {
        getSharedPreferences(context).edit().clear().apply();
    }

    public NavFloatingActionButton(Context context) {
        this(context, null);
    }

    public NavFloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        bindNavigationPad();
        mVibrationEnabled = Preferences.navigationVibrationEnabled(context);
        if (!isInEditMode()) {
            mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        } else {
            mVibrator = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
        mPreferenceObservable.subscribe(getContext(), (key, contextChanged) ->
                mVibrationEnabled = Preferences.navigationVibrationEnabled(getContext()),
                R.string.pref_navigation_vibrate);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopObservingViewTree();
        mPreferenceObservable.unsubscribe(getContext());
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        throw new UnsupportedOperationException();
    }

    public void setNavigable(Navigable navigable) {
        mNavigable = navigable;
    }

    @Synthetic
    void bindNavigationPad() {
        GestureDetectorCompat detectorCompat = new GestureDetectorCompat(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return mNavigable != null;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        Toast.makeText(getContext(), R.string.hint_nav_short,
                                Toast.LENGTH_LONG).show();
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        trackKonami(DOUBLE_TAP);
                        return super.onDoubleTap(e);
                    }

                    @Override
                    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1,
                                           float velocityX, float velocityY) {
                        int direction;
                        if (Math.abs(velocityX) > Math.abs(velocityY)) {
                            direction = velocityX < 0 ?
                                    Navigable.DIRECTION_LEFT : Navigable.DIRECTION_RIGHT;
                        } else {
                            direction = velocityY < 0 ?
                                    Navigable.DIRECTION_UP : Navigable.DIRECTION_DOWN;
                        }
                        mNavigable.onNavigate(direction);
                        if (mVibrationEnabled) {
                            mVibrator.vibrate(VIBRATE_DURATION_MS);
                        }
                        trackKonami(direction);
                        return false;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        if (mNavigable == null) {
                            return;
                        }
                        startDrag(e.getX(), e.getY());
                    }
                });
        //noinspection Convert2Lambda
        super.setOnTouchListener(new OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return detectorCompat.onTouchEvent(motionEvent);
            }
        });
    }

    @Synthetic
    void startDrag(float startX, float startY) {
        if (mVibrationEnabled) {
            mVibrator.vibrate(VIBRATE_DURATION_MS * 2);
        }
        Toast.makeText(getContext(), R.string.hint_drag, Toast.LENGTH_SHORT).show();
        //noinspection Convert2Lambda
        super.setOnTouchListener(new OnTouchListener() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        mMoved = true;
                        view.setX(motionEvent.getRawX() - startX); // TODO compensate shift
                        view.setY(motionEvent.getRawY() - startY);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        bindNavigationPad();
                        if (mMoved) {
                            persistPosition();
                        }
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
    }

    @Synthetic
    boolean trackKonami(int direction) {
        if (KONAMI_CODE[mNextKonamiCode] != direction) {
            mNextKonamiCode = direction == KONAMI_CODE[0] ? 1 : 0;
            return false;
        } else if (mNextKonamiCode == KONAMI_CODE.length - 1) {
            mNextKonamiCode = 0;
            if (mVibrationEnabled) {
                mVibrator.vibrate(new long[]{0, VIBRATE_DURATION_MS * 2,
                        100, VIBRATE_DURATION_MS * 2}, -1);
            }
            new AlertDialog.Builder(getContext())
                    .setView(R.layout.dialog_konami)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) ->
                            AppUtils.openPlayStore(getContext()))
                    .create()
                    .show();
            return true;
        } else {
            mNextKonamiCode++;
            return true;
        }
    }

    @Override
    public void onGlobalLayout() {
        restorePosition();
        stopObservingViewTree();
    }

    private void stopObservingViewTree() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getViewTreeObserver().removeOnGlobalLayoutListener(this);
        } else {
            //noinspection deprecation
            getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
    }

    @SuppressLint("CommitPrefEdits")
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Synthetic void persistPosition() {
        getPreferences()
                .edit()
                .putFloat(mPreferenceX, getX())
                .putFloat(mPreferenceY, getY())
                .apply();
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private void restorePosition() {
        setX(getPreferences().getFloat(mPreferenceX, getX()));
        setY(getPreferences().getFloat(mPreferenceY, getY()));
    }

    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Activity.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }

    private SharedPreferences getPreferences() {
        if (mPreferences == null) {
            mPreferences = getSharedPreferences(getContext());
            DisplayMetrics metrics = getDisplayMetrics();
            mPreferenceX = String.format(Locale.US, PREFERENCES_FAB_X,
                    getContext().getClass().getName(), metrics.widthPixels, metrics.heightPixels);
            mPreferenceY = String.format(Locale.US, PREFERENCES_FAB_Y,
                    getContext().getClass().getName(), metrics.widthPixels, metrics.heightPixels);
        }
        return mPreferences;
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(context.getPackageName() + PREFERENCES_FAB,
                Context.MODE_PRIVATE);
    }
}
