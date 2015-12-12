package io.github.hidroh.materialistic.test;

import android.view.animation.Animation;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(value = Animation.class, inheritImplementationMethods = true)
public class ShadowAnimation {
    private Animation.AnimationListener listener;

    @Implementation
    public void setAnimationListener(Animation.AnimationListener listener) {
        this.listener = listener;
    }

    public Animation.AnimationListener getAnimationListener() {
        return listener;
    }
}
