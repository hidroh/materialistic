package io.github.hidroh.materialistic.test;

import android.support.design.widget.FloatingActionButton;

import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowImageView;

@Implements(FloatingActionButton.class)
public class ShadowFloatingActionButton extends ShadowImageView {
    private boolean visible = true;

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }
}
