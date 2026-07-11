# Project Overview

この文書は Lite Memo の概要、主要な確認先、技術スタックをまとめます。

技術スタックの具体値は執筆時点のスナップショットです。バージョンの正確な値は、必ず
`app/build.gradle.kts` と `gradle/libs.versions.toml` を確認してください。

## 概要

Lite Memo は Android 向けの軽量メモアプリです。

現在の基本情報:

- パッケージ名: `com.appvoyager.litememo`
- ルートプロジェクト名: `Lite Memo`
- メインモジュール: `:app`
- ビルドフレーバー: `dev` / `prod`

## 主な確認先

- `app/`: Android アプリ本体
- `app/src/main/kotlin/com/appvoyager/litememo/`: Kotlin ソース
  - `ui/`: Compose 画面、ViewModel、UI state、ナビゲーション、テーマ
  - `domain/`: model、値オブジェクト、UseCase、Repository interface、provider
  - `data/`: Repository 実装、Room、DataStore、mapper、Hilt module、Export/Import
- `app/src/main/res/`: Android リソース（表示文字列は `strings.xml` に集約）
- `app/schemas/`: Room のエクスポート済みスキーマ
- `app/src/dev/google-services.json` / `app/src/prod/google-services.json`: Firebase 設定
- `gradle/libs.versions.toml`: 依存関係とプラグインのバージョン管理
- `.github/workflows/`: CI / CodeQL（設定済み）
- `fastlane/`: KtLint・テスト実行などの CI レーン

## 技術スタック

### Android / UI

- Kotlin
- Jetpack Compose
- Material 3
- Material 3 `NavigationBar` / Bottom Navigation
- Material Icons（Core）
- Navigation Compose
- Coil Compose（メモ画像サムネイル表示）
- ライト / ダークモード

### DI

- Hilt
- `hilt-lifecycle-viewmodel-compose`（Compose から ViewModel を注入）

### State / Async

- StateFlow
- Coroutines

### Data

- Room
- DataStore（Preferences）
- kotlinx.serialization（JSON。Export / Import に使用）

Room / DataStore の使い分けは [`docs/architecture.md`](architecture.md) の Data 方針を正本とします。

### Security

- androidx.biometric（アプリロック / 生体認証）

### Observability

- Firebase Crashlytics（クラッシュ収集）
- Google Services Gradle Plugin（flavor ごとの `google-services.json` を処理）

### Test

- JUnit 5（Unit Test）
- kotlinx-coroutines-test（`runTest`）
- MockK（モック）
- Turbine（`StateFlow` / `Channel` event の検証）
- Compose UI Test / Espresso / Room testing（instrumented test）
- Kover（カバレッジ計測）

### Build

- JDK 17（jvmToolchain）
- compileSdk 36 / minSdk 28 / targetSdk 36
- release ビルド: R8 minify + リソース圧縮 + ProGuard
- Crashlytics Gradle Plugin（release の mapping file upload）
- KtLint（コード整形）
- detekt（静的解析。書き方・複雑度 + Compose 特化ルール `io.nlopez.compose.rules`。baseline 運用）
- Android Lint（Android 特有の問題検出。`warningsAsErrors`。baseline なし）

### CI

- GitHub Actions
  - CI: Static Analysis（KtLint / detekt / Android Lint）/ Unit Test / Android Test
  - Gradle Wrapper Validation（wrapper の改ざん検知）
  - actionlint（ワークフロー検査）
  - CodeQL、Dependabot
- fastlane（`ci` / `ktlint` / `detekt` / `lint` / `unit_test` / `android_test` レーン）

### Monetization / Release

- AdMob（Google Mobile Ads SDK）
  - バナー: `LiteMemoApp` のボトムナビゲーション上部に、全タブ共通のアンカー型アダプティブバナーを表示
  - アプリ ID / 広告ユニット ID は flavor 別の `strings.xml` で管理
    - `dev`: Google 公式テスト ID（`app/src/dev/res`）
    - `prod`: 本番 ID（`app/src/prod/res`）

署名鍵・ストアパスワードなどリリース署名に関する秘密情報はコミットしません。

### Localization

- 日本語 / 英語対応
- 表示文字列はリソース化する
- 文字列の直書きは避ける

## 判断の前提

- まず Gradle とソースを見て、現在導入済みのものを確認する
- 既存構成を優先し、必要以上に大きな再編をしない
- 構造方針は Clean Architecture + MVVM とし、詳細は `docs/architecture.md` を確認する
