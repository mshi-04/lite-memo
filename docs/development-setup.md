# Development Setup

クローン後に一度だけ実行が必要な設定と、開発時の前提をまとめます。

## 前提

- JDK 17（Gradle の `jvmToolchain` は 17）
- ビルドフレーバーは `dev` / `prod`。開発・動作確認は `dev` を使う

## Git フックの有効化

pre-push フックとして KtLint が設定されています。
クローン後に以下を実行してフックを有効にしてください。

```sh
git config core.hooksPath .githooks
```

これにより、`git push` 時に自動で KtLint が実行され、違反があるとプッシュが中断されます。

## ローカルでの CI 相当チェック（任意）

CI と同じ KtLint / Unit Test を手元で流す場合は fastlane を使えます。

```sh
bundle exec fastlane android ci
```
