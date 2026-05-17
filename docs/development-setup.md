# Development Setup

クローン後に一度だけ実行が必要な設定をまとめます。

## Git フックの有効化

pre-push フックとして KtLint が設定されています。
クローン後に以下を実行してフックを有効にしてください。

```sh
git config core.hooksPath .githooks
```

これにより、`git push` 時に自動で KtLint が実行され、違反があるとプッシュが中断されます。
