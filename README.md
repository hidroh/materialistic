## Materialistic for Hacker News
Material design [Hacker News] client for Android, uses official [HackerNews/API], [Dagger] for dependency injection and [Robolectric] for unit testing.

[![Get it on Google Play][Play Store Badge]][Play Store]

### Setup
**Requirements**
- JDK 8
- Latest Android SDK tools
- Latest Android platform tools
- Android SDK Build tools 23.0.3
- Android SDK 23
- Android Support Repository
- Android Support Library 23.3.0

**Dependencies**
- [Official Hacker News API][HackerNews/API], user services (e.g. login/create account/vote/comment) rely on redirect requests to Hacker News website
- [Algolia Hacker News Search API]
- [Readability Parser API]
- AOSP [appcompat-v7] / [recyclerview-v7] / [design] / [cardview-v7] / [preference-v7] / [customtabs]
- Square [Retrofit] / [OkHttp] / [AssertJ] / [Dagger] / [LeakCanary]
- [Retrolambda] & [Retrolambda Gradle plugin]
- [Robolectric]

**Build**

    ./gradlew assembleDebug

Build with LeakCanary on

    ./gradlew assembleLeak

Grab your Readability Parser API key [here][readability] if you want to connect to Readability.

**Test** [![Build Status]][Travis]

Run all/selective tests:

    ./gradlew testDebug
    ./gradlew testDebug --tests "*HackerNewsClientTest"

**Coverage** [![Coverage Status]][Coveralls]

    ./gradlew jacocoTestReport

### Articles
- [Supporting multiple themes in your Android app (Part 1)][article-theme1]
- [Supporting multiple themes in your Android app (Part 2)][article-theme2] [![][Android Weekly 144 Badge]][Android Weekly 144]
- [Building custom preferences with preference-v7][article-preference]

### Screenshots
<img src="assets/screenshot-1.png" width="200px" />
<img src="assets/screenshot-2.png" width="200px" />
<img src="assets/screenshot-3.png" width="200px" />
<img src="assets/screenshot-4.png" width="600px" />

### Contributing
Contributions are always welcome. Please make sure you read [Contributing notes](CONTRIBUTING.md) first.

### License
    Copyright 2015 Ha Duy Trung
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[Build Status]: https://travis-ci.org/hidroh/materialistic.svg?branch=master
[Travis]: https://travis-ci.org/hidroh/materialistic
[Coverage Status]: https://coveralls.io/repos/hidroh/materialistic/badge.svg?branch=master
[Coveralls]: https://coveralls.io/r/hidroh/materialistic?branch=master
[Hacker News]: https://news.ycombinator.com/
[HackerNews/API]: https://github.com/HackerNews/API
[Play Store]: https://play.google.com/store/apps/details?id=io.github.hidroh.materialistic&referrer=utm_source%3Dgithub
[Play Store Badge]: https://play.google.com/intl/en_us/badges/images/badge_new.png
[Algolia Hacker News Search API]: https://github.com/algolia/hn-search
[Readability Parser API]: https://www.readability.com/developers/api/parser
[appcompat-v7]: https://developer.android.com/tools/support-library/features.html#v7-appcompat
[recyclerview-v7]: https://developer.android.com/tools/support-library/features.html#v7-recyclerview
[design]: https://developer.android.com/tools/support-library/features.html#design
[cardview-v7]: https://developer.android.com/tools/support-library/features.html#v7-cardview
[preference-v7]: https://developer.android.com/tools/support-library/features.html#v7-preference
[customtabs]: https://developer.android.com/tools/support-library/features.html#custom-tabs
[Retrofit]: https://github.com/square/retrofit
[OkHttp]: https://github.com/square/okhttp
[AssertJ]: https://github.com/square/assertj-android
[Dagger]: https://github.com/square/dagger
[LeakCanary]: https://github.com/square/leakcanary
[Retrolambda]: https://github.com/orfjackal/retrolambda
[Retrolambda Gradle plugin]: https://github.com/evant/gradle-retrolambda
[Robolectric]: https://github.com/robolectric/robolectric
[readability]: https://www.readability.com/developers/api/parser
[article-theme1]: http://www.hidroh.com/2015/02/16/support-multiple-themes-android-app/
[article-theme2]: http://www.hidroh.com/2015/02/25/support-multiple-themes-android-app-part-2/
[article-preference]: http://www.hidroh.com/2015/11/30/building-custom-preferences-v7/
[Android Weekly 144 Badge]: https://img.shields.io/badge/android--weekly-144-blue.svg
[Android Weekly 144]: http://androidweekly.net/issues/issue-144