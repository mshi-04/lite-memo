# AGENTS.md

Lite Memo で AI エージェントが最初に読む入口です。
ここには地図だけを書き、詳細は `docs/` 配下の文書へ分けます。

## 技術文書

- [`docs/project-overview.md`](docs/project-overview.md): プロジェクト概要と主要な確認先
- [`docs/tech-stack.md`](docs/tech-stack.md): 導入予定の技術スタック
- [`docs/architecture.md`](docs/architecture.md): Clean Architecture / MVVM の構造方針
- [`docs/implementation-guidelines.md`](docs/implementation-guidelines.md): 実装時の基本方針
- [`docs/unit-test.md`](docs/unit-test.md): Unit Test の方針
- [`docs/review.md`](docs/review.md): コードレビューの形式

## AI作業用 Skill

作業の種類に応じて、必要な Skill を最初に確認します。

- [`.agents/skills/ui-implementation/SKILL.md`](.agents/skills/ui-implementation/SKILL.md): Compose / ViewModel / UI state / 画面テスト
- [`.agents/skills/domain-implementation/SKILL.md`](.agents/skills/domain-implementation/SKILL.md): model / value object / UseCase / Repository interface
- [`.agents/skills/data-implementation/SKILL.md`](.agents/skills/data-implementation/SKILL.md): Repository 実装 / mapper / DataStore / export-import
- [`.agents/skills/db-implementation/SKILL.md`](.agents/skills/db-implementation/SKILL.md): Room entity / DAO / migration / schema
- [`.agents/skills/test-implementation/SKILL.md`](.agents/skills/test-implementation/SKILL.md): Unit Test / androidTest / coroutine / Flow 検証
- [`.agents/skills/implementation-review/SKILL.md`](.agents/skills/implementation-review/SKILL.md): 実装後レビューと指摘整理
- [`.agents/skills/ci-build-troubleshooting/SKILL.md`](.agents/skills/ci-build-troubleshooting/SKILL.md): Gradle / 静的解析 / CI 失敗調査
- [`.agents/skills/agent-docs-maintenance/SKILL.md`](.agents/skills/agent-docs-maintenance/SKILL.md): AI 文書 / skill / Claude Code 同期運用

## 最低限の前提

- Lite Memo は Android 向けの軽量メモアプリ
- パッケージ名は `com.appvoyager.litememo`
- メインモジュールは `:app`
- UI は Kotlin / Jetpack Compose / Material 3 を軸にする
- 構造は Clean Architecture + MVVM を軸にする
- 未導入の技術は、実装済みとして扱わない
