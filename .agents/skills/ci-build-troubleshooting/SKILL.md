---
name: ci-build-troubleshooting
description: Lite Memo のビルド、静的解析、CI、Gradle、fastlane、GitHub Actions、ktlint、detekt、Android Lint、Kover、CodeQL、テスト失敗を調査または修正するときに使う。
---

# 目的

Lite Memo のローカル検証と CI 失敗を、既存の Gradle / fastlane / GitHub Actions 構成に沿って切り分ける。

# 最初に確認するもの

- [`docs/development-setup.md`](../../../docs/development-setup.md)
- [`docs/tech-stack.md`](../../../docs/tech-stack.md)
- [`app/build.gradle.kts`](../../../app/build.gradle.kts)
- [`gradle/libs.versions.toml`](../../../gradle/libs.versions.toml)
- [`.github/workflows/`](../../../.github/workflows/)
- [`fastlane/`](../../../fastlane/)
- [`config/detekt/`](../../../config/detekt/)

# 手順

1. 失敗している job、task、variant、ログの最初の実エラーを確認する。
2. ktlint は整形、detekt は書き方・複雑度、Android Lint は Android 固有問題として切り分ける。
3. 変更範囲に近い最小 task から再現し、必要に応じて CI 相当の fastlane lane を使う。
4. pre-commit hook や staged file 限定 task の挙動を変更する場合は、未ステージ差分の混入に注意する。
5. 依存追加や version 更新では `gradle/libs.versions.toml`、KSP、plugin、test dependency の整合を確認する。
6. CI 設定変更では PR キャッシュ、main/develop キャッシュ、merge ref、flavor、runner 制約を確認する。

# 注意事項

- formatter や baseline 更新など repo-tracked file を書き換える command は意図を確認してから実行する。
- 既存 baseline で吸収されている違反と新規違反を混同しない。
- アプリ実装の問題と CI 設定の問題を分けて報告する。
