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

package io.github.hidroh.materialistic.test.shadow;

import androidx.drawerlayout.widget.DrawerLayout;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowViewGroup;

import java.util.ArrayList;
import java.util.List;

import static org.robolectric.shadow.api.Shadow.directlyOn;

@Implements(DrawerLayout.class)
public class ShadowSupportDrawerLayout extends ShadowViewGroup {
    @RealObject
    private DrawerLayout realDrawerLayout;
    private List<DrawerLayout.DrawerListener> drawerListeners = new ArrayList<>();

    @Implementation
    public void addDrawerListener(DrawerLayout.DrawerListener drawerListener) {
        this.drawerListeners.add(drawerListener);
        directlyOn(realDrawerLayout, DrawerLayout.class).addDrawerListener(drawerListener);
    }

    @Implementation
    public void removeDrawerListener(DrawerLayout.DrawerListener drawerListener) {
        this.drawerListeners.add(drawerListener);
        directlyOn(realDrawerLayout, DrawerLayout.class).removeDrawerListener(drawerListener);
    }

    public List<DrawerLayout.DrawerListener> getDrawerListeners() {
        return drawerListeners;
    }
}
