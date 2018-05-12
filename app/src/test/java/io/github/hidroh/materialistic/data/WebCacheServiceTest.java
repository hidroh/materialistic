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

package io.github.hidroh.materialistic.data;

import android.app.Service;
import android.content.Intent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;

import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.ShadowWebView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = ShadowWebView.class)
@RunWith(TestRunner.class)
public class WebCacheServiceTest {
    private ServiceController<WebCacheService> controller;
    private WebCacheService service;

    @Before
    public void setUp() {
        controller = Robolectric.buildService(WebCacheService.class);
        service = controller.create().get();
    }

    @Test
    public void testWebCache() {
        controller = Robolectric.buildService(WebCacheService.class,
                new Intent()
                        .putExtra(WebCacheService.EXTRA_URL, "http://example.com"));
        ShadowWebView.lastGlobalLoadedUrl = null;
        controller.startCommand(0, 0);
        service = controller.create().get();
        assertThat(ShadowWebView.getLastGlobalLoadedUrl()).contains("http://example.com");

    }

    @Test
    public void testRestartNullIntent() {
        service.onStartCommand(null, Service.START_FLAG_REDELIVERY, 0);
        assertThat(shadowOf(service).isStoppedBySelf()).isTrue();
    }

    @After
    public void tearDown() {
        controller.destroy();
    }
}
