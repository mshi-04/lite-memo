# Development Setup

クローン後に一度だけ実行が必要な設定と、開発時の前提をまとめます。

## 前提

- JDK 17（Gradle の `jvmToolchain` は 17）
- ビルドフレーバーは `dev` / `prod`。開発・動作確認は `dev` を使う

## Firebase / Crashlytics

Crashlytics は `dev` / `prod` の両方に導入済みです。
Firebase 設定ファイルは flavor ごとに配置します。

- `app/src/dev/google-services.json`: `com.appvoyager.litememo.dev`
- `app/src/prod/google-services.json`: `com.appvoyager.litememo`

`app/google-services.json` は全 variant の fallback になるため、flavor 固有の設定ファイルは `src/<flavor>/` 配下に置きます。
release ビルドは R8 対象で、Crashlytics Gradle Plugin が release variant の mapping file upload task を生成します。

## Git フックの有効化

pre-commit フックとして KtLint 整形 + detekt 検査が設定されています。
クローン後に以下を実行してフックを有効にしてください。

```sh
git config core.hooksPath .githooks
```

これにより、`git commit` 時にステージ済みの Kotlin ファイルがある場合だけ、**そのステージ済みファイルに対して** KtLint 整形（`ktlintFormatPreCommit`）と detekt 検査（`detektPreCommit`）が実行されます。フックは Gradle デーモンとビルドキャッシュ（`org.gradle.caching=true`）を利用するため、2 回目以降は高速化されます。

- Kotlin ファイルを含むコミットで未ステージの tracked 変更がある場合は、意図しない整形や混入を避けるためコミットが中断されます。
- 整形後は、コミット開始時点でステージされていたファイルだけを再ステージします。KtLint がステージ外のファイルも変更した場合は、内容を確認してから再度コミットしてください。
- detekt が違反を検出した場合はコミットが中断されます。`app/build/reports/detekt/` のレポートを確認して修正してください（detekt は自動修正しません）。
- detekt は baseline（`config/detekt/baseline.xml`）を適用するため、既存違反では止まりません（新規違反のみ検出）。

## 静的解析（ktlint / detekt / Android Lint）

役割分担は次のとおりです。

- **ktlint**: コード整形（フォーマット）
- **detekt**: 書き方・複雑度・アンチパターン（+ Compose 特化ルール）
- **Android Lint**: Android 特有のバグ・非推奨 API・リソース・アクセシビリティ

detekt と Android Lint は既存違反を baseline で吸収し、CI は新規違反のみで失敗します。

- detekt baseline: `config/detekt/baseline.xml`
- Android Lint baseline: `app/lint-baseline.xml`

ルールを直したうえで baseline を更新したい場合は、対象 baseline を削除してから再生成します。

```sh
# detekt baseline の再生成
./gradlew detektBaseline

# Android Lint baseline の再生成（既存ファイルを削除してから実行）
./gradlew :app:lintProdDebug
```

## ローカルでの CI 相当チェック（任意）

CI と同じ静的解析 / Unit Test を手元で流す場合は fastlane を使えます。

```sh
# まとめて（KtLint → detekt → Android Lint → Unit Test）
bundle exec fastlane android ci

# 個別に
bundle exec fastlane android ktlint
bundle exec fastlane android detekt
bundle exec fastlane android lint
```

Gradle から直接実行する場合は次のとおりです。

```sh
./gradlew app:ktlintCheck
./gradlew detekt
./gradlew :app:lintProdDebug
```
