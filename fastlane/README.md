fastlane documentation
----

# Installation

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android ktlint

```sh
[bundle exec] fastlane android ktlint
```

Run KtLint

### android detekt

```sh
[bundle exec] fastlane android detekt
```

Run detekt

### android lint

```sh
[bundle exec] fastlane android lint
```

Run Android Lint (prod)

### android static_analysis

```sh
[bundle exec] fastlane android static_analysis
```

Run static analysis (KtLint + detekt + Android Lint)

### android unit_test

```sh
[bundle exec] fastlane android unit_test
```

Run unit tests (debug only by default; pass release:true to also verify the release build)

### android coverage

```sh
[bundle exec] fastlane android coverage
```

Generate Kover XML and HTML reports for prodDebug

### android ci

```sh
[bundle exec] fastlane android ci
```

Run CI checks (KtLint + detekt + Android Lint + Unit Test)

### android android_test

```sh
[bundle exec] fastlane android android_test
```

Run Android UI/Instrumentation tests

----

This README.md is generated from the Fastfile. Automatic regeneration during lane execution is disabled; update this file together with the Fastfile lane descriptions.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
