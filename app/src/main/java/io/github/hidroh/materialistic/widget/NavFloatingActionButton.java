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
import android.content.Context;
import android.os.Build;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.Navigable;
import io.github.hidroh.materialistic.R;

public class NavFloatingActionButton extends FloatingActionButton {
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
    private final Vibrator mVibrator;
    private Navigable mNavigable;
    private int mNextKonamiCode = 0;

    public NavFloatingActionButton(Context context) {
        this(context, null);
    }

    public NavFloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        bindNavigationPad();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        throw new UnsupportedOperationException();
    }

    public void setNavigable(Navigable navigable) {
        mNavigable = navigable;
    }

    private void bindNavigationPad() {
        GestureDetectorCompat detectorCompat = new GestureDetectorCompat(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
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
                        if (mNavigable == null) {
                            return false;
                        }
                        int direction;
                        if (Math.abs(velocityX) > Math.abs(velocityY)) {
                            direction = velocityX < 0 ?
                                    Navigable.DIRECTION_LEFT : Navigable.DIRECTION_RIGHT;
                        } else {
                            direction = velocityY < 0 ?
                                    Navigable.DIRECTION_UP : Navigable.DIRECTION_DOWN;
                        }
                        mNavigable.onNavigate(direction);
                        mVibrator.vibrate(VIBRATE_DURATION_MS);
                        trackKonami(direction);
                        return false;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                            Toast.makeText(getContext(), R.string.not_supported, Toast.LENGTH_SHORT).show();
                        } else {
                            startDrag(e.getX(), e.getY());
                        }
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

    private void startDrag(float startX, float startY) {
        mVibrator.vibrate(VIBRATE_DURATION_MS * 2);
        Toast.makeText(getContext(), R.string.hint_drag, Toast.LENGTH_SHORT).show();
        //noinspection Convert2Lambda
        super.setOnTouchListener(new OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        view.setX(motionEvent.getRawX() - startX); // TODO compensate shift
                        view.setY(motionEvent.getRawY() - startY);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        bindNavigationPad();
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
    }

    private boolean trackKonami(int direction) {
        if (KONAMI_CODE[mNextKonamiCode] != direction) {
            mNextKonamiCode = direction == KONAMI_CODE[0] ? 1 : 0;
            return false;
        } else if (mNextKonamiCode == KONAMI_CODE.length - 1) {
            mNextKonamiCode = 0;
            mVibrator.vibrate(new long[]{0, VIBRATE_DURATION_MS * 2,
                    100, VIBRATE_DURATION_MS * 2}, -1);
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
}
