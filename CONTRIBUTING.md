## Contributing

**Translations**

Please translate only string resources specified in [`strings.xml`](app/src/main/res/values/strings.xml). Others have been marked as non-translatable. It is recommended that you use [Android Studio Translations Editor](http://tools.android.com/recent/androidstudio087released) to check for missing translations.

**Lint checking**

Lint is turned on and has been set to be very strict, failing build for either warnings or errors. Please make sure your changes do not violate any Lint checks. Exceptions (ignore/severity override) can be made on case by case basis.

**Code style**

Though not strictly enforced, it is strongly recommended that you follow [Android code style](https://source.android.com/source/code-style.html), or at the very least follow the existing code style where you make changes for consistency. If needed, please separate commits for code style and business logic changes.