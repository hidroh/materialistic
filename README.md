[![Build Status](https://travis-ci.org/hidroh/materialistic.svg?branch=master)](https://travis-ci.org/hidroh/materialistic) [![Coverage Status](https://coveralls.io/repos/hidroh/materialistic/badge.svg?branch=master)](https://coveralls.io/r/hidroh/materialistic?branch=master) [![Join the chat at https://gitter.im/hidroh/materialistic](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/hidroh/materialistic?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Get it on Google Play](https://developer.android.com/images/brand/en_generic_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=io.github.hidroh.materialistic)

## Hacker News client for Android - Material Design

### Setup
**Requirements**
- Latest Android SDK tools
- Latest Android platform tools
- Android SDK Build tools 21.1.2
- Android SDK 21
- Android Support Repository
- Android Support Library 21.0.3
- Google Repository
- Google Play services

**Dependencies**
- [Official Hacker News API](https://github.com/HackerNews/API)
- [appcompat-v7](https://developer.android.com/tools/support-library/features.html#v7-appcompat) / [recyclerview-v7](https://developer.android.com/tools/support-library/features.html#v7-recyclerview) / [cardview-v7](https://developer.android.com/tools/support-library/features.html#v7-cardview)
- Square [Retrofit](https://github.com/square/retrofit) / [OkHttp](https://github.com/square/okhttp)
- [Square AssertJ](https://github.com/square/assertj-android)
- [Robolectric](https://github.com/robolectric/robolectric)

**Build**

    ./gradlew assembleDebug

Supply your own release signing config to build release. Release signing config is left out on purpose.

**Test**

    ./gradlew test

**Coverage**

    ./gradlew jacocoTestReport

### Screenshots
<img src="assets/screenshot-1.png" width="300px" />
<img src="assets/screenshot-2.png" width="300px" />
<img src="assets/screenshot-3.png" width="300px" />
<img src="assets/screenshot-4.png" width="300px" />
<img src="assets/screenshot-5.png" width="600px" />
<img src="assets/screenshot-6.png" width="600px" />
