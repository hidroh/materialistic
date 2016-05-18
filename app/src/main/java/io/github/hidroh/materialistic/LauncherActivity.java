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
import android.content.Intent;
import android.os.Bundle;

import java.util.HashMap;

public class LauncherActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HashMap<String, Class<? extends Activity>> map = new HashMap<>();
        map.put(getString(R.string.pref_launch_screen_value_top), ListActivity.class);
        map.put(getString(R.string.pref_launch_screen_value_best), BestActivity.class);
        map.put(getString(R.string.pref_launch_screen_value_hot), PopularActivity.class);
        map.put(getString(R.string.pref_launch_screen_value_new), NewActivity.class);
        map.put(getString(R.string.pref_launch_screen_value_ask), AskActivity.class);
        map.put(getString(R.string.pref_launch_screen_value_show), ShowActivity.class);
        map.put(getString(R.string.pref_launch_screen_value_jobs), JobsActivity.class);
        map.put(getString(R.string.pref_launch_screen_value_saved), FavoriteActivity.class);
        String launchScreen = Preferences.getLaunchScreen(this);
        startActivity(new Intent(this, map.containsKey(launchScreen) ?
                map.get(launchScreen) : ListActivity.class));
        finish();
    }
}
