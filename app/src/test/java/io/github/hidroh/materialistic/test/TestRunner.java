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

package io.github.hidroh.materialistic.test;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.GradleManifestFactory;
import org.robolectric.internal.ManifestFactory;
import org.robolectric.internal.ManifestIdentifier;
import org.robolectric.res.FileFsFile;

public class TestRunner extends RobolectricTestRunner {

    public TestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected ManifestFactory getManifestFactory(Config config) {
        if (config.constants() != Void.class) {
            return new GradleManifestFactory() {
                private static final String SRC = "src/main";

                @Override
                public ManifestIdentifier identify(Config config) {
                    ManifestIdentifier identifier = super.identify(config);
                    if (identifier.getAssetDir().exists()) {
                        return identifier;
                    } else {
                        return new ManifestIdentifier(identifier.getManifestFile(),
                                identifier.getResDir(),
                                FileFsFile.from(SRC, "assets"),
                                identifier.getPackageName(),
                                identifier.getLibraryDirs());
                    }
                }
            };
        } else {
            return super.getManifestFactory(config);
        }
    }

    @Override
    protected Config buildGlobalConfig() {
        return new Config.Builder()
                .setBuildDir(System.getProperty("robolectric.buildDir"))
                .build();
    }
}
