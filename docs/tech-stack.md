# Tech Stack

この文書は Lite Memo の技術スタックをまとめます。
バージョンの正確な値は、必ず `app/build.gradle.kts` と `gradle/libs.versions.toml` を確認してください。

## Android / UI

- Kotlin
- Jetpack Compose
- Material 3
- Material 3 `NavigationBar` / Bottom Navigation
- Material Icons（Core）
- Navigation Compose
- ライト / ダークモード

## DI

- Hilt
- `hilt-lifecycle-viewmodel-compose`（Compose から ViewModel を注入）

## State / Async

- StateFlow
- Coroutines

## Data

- Room（構造化データ。migration とスキーマエクスポートあり）
- DataStore（Preferences）
- kotlinx.serialization（JSON。Export / Import に使用）

Room はメモ・タグなどの構造化データに使います。
DataStore はテーマ、表示設定、メモ編集の下書きなどの軽量な設定値に使います。

## Security

- androidx.biometric（アプリロック / 生体認証）

## Test

- JUnit 5（Unit Test）
- kotlinx-coroutines-test（`runTest`）
- Compose UI Test / Espresso / Room testing（instrumented test）

## Build

- JDK 17（jvmToolchain）
- compileSdk 36 / minSdk 28 / targetSdk 36
- productFlavors: `dev` / `prod`
- release ビルド: minify + リソース圧縮 + ProGuard
- KtLint（コードスタイル）

## CI

- GitHub Actions（CI: KtLint / Unit Test / Android Test、CodeQL、Dependabot）
- fastlane（`ci` / `ktlint` / `unit_test` / `android_test` レーン）

## Monetization / Release（予定）

- AdMob（未導入）
- Google Play 公開（未導入）

AdMob、署名、Google Play、リリース設定に関係する秘密情報はコミットしません。

## Localization

- 日本語 / 英語対応
- 表示文字列はリソース化する
- 文字列の直書きは避ける
