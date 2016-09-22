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
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.FileFsFile;
import org.robolectric.util.Logger;
import org.robolectric.util.ReflectionHelpers;

public class RobolectricGradleTestRunner extends org.robolectric.RobolectricGradleTestRunner {
    private static final String BUILD_OUTPUT = "build/intermediates";
    private static final String SRC = "src/main";

    public RobolectricGradleTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected AndroidManifest getAppManifest(Config config) {
        if (config.constants() == Void.class) {
            Logger.error("Field 'constants' not specified in @Config annotation");
            Logger.error("This is required when using RobolectricGradleTestRunner!");
            throw new RuntimeException("No 'constants' field in @Config annotation!");
        }

        final String type = getType(config);
        final String flavor = getFlavor(config);
        final String packageName = getPackageName(config);

        final FileFsFile res;
        final FileFsFile assets;
        final FileFsFile manifest;

        // res/merged added in Android Gradle plugin 1.3-beta1
        if (FileFsFile.from(BUILD_OUTPUT, "res", "merged").exists()) {
            res = FileFsFile.from(BUILD_OUTPUT, "res", "merged", flavor, type);
        } else if (FileFsFile.from(BUILD_OUTPUT, "res").exists()) {
            res = FileFsFile.from(BUILD_OUTPUT, "res", flavor, type);
        } else {
            res = FileFsFile.from(BUILD_OUTPUT, "bundles", flavor, type, "res");
        }

        if (FileFsFile.from(BUILD_OUTPUT, "assets").exists()) {
            assets = FileFsFile.from(BUILD_OUTPUT, "assets", flavor, type);
        } else if (FileFsFile.from(BUILD_OUTPUT, "bundles", flavor, type, "assets").exists()) {
            assets = FileFsFile.from(BUILD_OUTPUT, "bundles", flavor, type, "assets");
        } else {
            assets = FileFsFile.from(SRC, "assets");
        }

        if (FileFsFile.from(BUILD_OUTPUT, "manifests").exists()) {
            manifest = FileFsFile.from(BUILD_OUTPUT, "manifests", "full", flavor, type, "AndroidManifest.xml");
        } else {
            manifest = FileFsFile.from(BUILD_OUTPUT, "bundles", flavor, type, "AndroidManifest.xml");
        }

        Logger.debug("Robolectric assets directory: " + assets.getPath());
        Logger.debug("   Robolectric res directory: " + res.getPath());
        Logger.debug("   Robolectric manifest path: " + manifest.getPath());
        Logger.debug("    Robolectric package name: " + packageName);
        return new AndroidManifest(manifest, res, assets, packageName);
    }

    private static String getType(Config config) {
        try {
            return ReflectionHelpers.getStaticField(config.constants(), "BUILD_TYPE");
        } catch (Throwable e) {
            return null;
        }
    }

    private static String getFlavor(Config config) {
        try {
            return ReflectionHelpers.getStaticField(config.constants(), "FLAVOR");
        } catch (Throwable e) {
            return null;
        }
    }

    private static String getPackageName(Config config) {
        try {
            final String packageName = config.packageName();
            if (packageName != null && !packageName.isEmpty()) {
                return packageName;
            } else {
                return ReflectionHelpers.getStaticField(config.constants(), "APPLICATION_ID");
            }
        } catch (Throwable e) {
            return null;
        }
    }
}
