# AGENTS.md

Lite Memo で AI エージェントが最初に読む入口です。
ここには地図だけを書き、詳細は `docs/` 配下の文書へ分けます。

## 最初に読む文書

- `docs/project-overview.md`: プロジェクト概要と主要な確認先
- `docs/tech-stack.md`: 導入予定の技術スタック
- `docs/architecture.md`: Clean Architecture / MVVM の構造方針
- `docs/implementation-guidelines.md`: 実装時の基本方針
- `docs/sub-agent-guidelines.md`: サブエージェント運用の方針
- `docs/unit-test.md`: Unit Test の方針
- `docs/review.md`: コードレビューの形式

## AI作業用 Skill

作業の種類に応じて、必要な Skill を最初に確認します。

- `.agents/skills/design-feature/SKILL.md`: 実装前の設計整理
- `.agents/skills/implement-feature/SKILL.md`: 既存方針に沿った実装
- `.agents/skills/review-implementation/SKILL.md`: 実装後のレビュー

## 最低限の前提

- Lite Memo は Android 向けの軽量メモアプリ
- パッケージ名は `com.appvoyager.litememo`
- メインモジュールは `:app`
- UI は Kotlin / Jetpack Compose / Material 3 を軸にする
- 構造は Clean Architecture + MVVM を軸にする
- 未導入の技術は、実装済みとして扱わない
