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

import android.os.Bundle;

import dagger.ObjectGraph;

public abstract class InjectableActivity extends ThemedActivity implements Injectable {
    private ObjectGraph mActivityGraph;
    private boolean mDestroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inject(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
        mActivityGraph = null;
    }

    @Override
    public void onBackPressed() {
        // TODO http://b.android.com/176265
        try {
            super.onBackPressed();
        } catch (IllegalStateException e) {
            supportFinishAfterTransition();
        }
    }

    @Override
    public void inject(Object object) {
        getApplicationGraph().inject(object);
    }

    @Override
    public ObjectGraph getApplicationGraph() {
        if (mActivityGraph == null) {
            mActivityGraph = ((Injectable) getApplication()).getApplicationGraph()
                    .plus(new ActivityModule(this), new UiModule());
        }
        return mActivityGraph;
    }

    public boolean isActivityDestroyed() {
        return mDestroyed;
    }
}
