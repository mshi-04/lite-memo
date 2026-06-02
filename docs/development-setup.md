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

pre-commit フックとして KtLint が設定されています。
クローン後に以下を実行してフックを有効にしてください。

```sh
git config core.hooksPath .githooks
```

これにより、`git commit` 時にステージ済みの Kotlin ファイルがある場合だけ KtLint（ktlintFormat）が実行されます。Kotlin ファイルを整形する必要があるコミットで未ステージの tracked 変更がある場合は、意図しない整形や混入を避けるためコミットが中断されます。

整形後は、コミット開始時点でステージされていたファイルだけを再ステージします。KtLint がステージ外のファイルも変更した場合は、内容を確認してから再度コミットしてください。整形に失敗した場合は、エラーメッセージを確認して手動修正するか、ローカルで ktlintFormat を再実行してください。

## ローカルでの CI 相当チェック（任意）

CI と同じ KtLint / Unit Test を手元で流す場合は fastlane を使えます。

```sh
bundle exec fastlane android ci
```
