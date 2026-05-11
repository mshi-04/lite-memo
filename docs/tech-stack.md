# Tech Stack

この文書は Lite Memo の導入予定スタックをまとめます。
導入済みかどうかは、必ず `app/build.gradle.kts` と `gradle/libs.versions.toml` を確認してください。

## Android / UI

- Kotlin
- Jetpack Compose
- Material 3
- Material 3 `NavigationBar` / Bottom Navigation
- Navigation Compose
- ライト / ダークモード

## DI

- Hilt

## State / Async

- StateFlow
- Coroutines

## Test

- JUnit 5

## Data

- Room
- DataStore

Room はメモなどの構造化データに使います。
DataStore はテーマ、表示設定、初回起動状態などの軽量な設定値に使います。

## Monetization / Release

- AdMob
- GitHub Actions
- Google Play 公開

AdMob、署名、Google Play、リリース設定に関係する秘密情報はコミットしません。

## Localization

- 日本語 / 英語対応
- 表示文字列はリソース化する
- 文字列の直書きは避ける
