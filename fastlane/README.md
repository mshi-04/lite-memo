fastlane documentation
----

# Installation

## Android prerequisites

For Android CI, make sure the environment has the following installed:

- JDK 17
- Android SDK with `ANDROID_HOME` or `ANDROID_SDK_ROOT` set
- Android SDK Platform-Tools
- Android SDK Build-Tools required by the app build

See the [Android Studio command-line tools documentation](https://developer.android.com/studio/command-line) for Android SDK setup details.

## macOS / iOS prerequisites

If you are on macOS and need iOS-related lanes, make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane).

# Available Actions

## Android

### android ci

```sh
[bundle exec] fastlane android ci
```

Run KtLint and unit tests

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
