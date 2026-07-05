---
name: agent-docs-maintenance
description: Lite Memo の AI 向け文書、AGENTS.md、CLAUDE.md、docs、.agents/skills、.claude/skills、skill 同期運用を追加または更新するときに使う。
---

# 目的

Lite Memo の AI 作業導線を、薄い入口文書と詳細 docs / skills の分担を保って更新する。

# 最初に確認するもの

- [`AGENTS.md`](../../../AGENTS.md)
- [`CLAUDE.md`](../../../CLAUDE.md)
- [`docs/claude-code.md`](../../../docs/claude-code.md)
- [`docs/project-overview.md`](../../../docs/project-overview.md)
- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md)
- [`.agents/skills/`](..)
- [`scripts/sync-claude-skills.ps1`](../../../scripts/sync-claude-skills.ps1)

# 手順

1. 入口に置く情報と、docs / skill に分ける情報を整理する。
2. `AGENTS.md` は地図として薄く保ち、詳細手順は docs または `.agents/skills/` に置く。
3. skill は作業種類ごとの入口にし、長い規約本文を重複させず参照先を明記する。
4. `.agents/skills/` を正本として編集する。
5. skill を追加・変更・削除したら `scripts/sync-claude-skills.ps1` で `.claude/skills/` を生成する。
6. skill を増減した場合は `AGENTS.md` の skill 一覧も更新する。
7. `scripts/sync-claude-skills.ps1 -Check` で同期漏れと AGENTS.md 一覧のずれを確認する。

# 注意事項

- `.claude/skills/` 配下の同期対象 skill は直接編集しない。
- marker file や二重管理の仕組みを増やさない。
- 未導入の技術を実装済みとして書かない。
- アプリ実装ファイルを変更しない docs 作業では、Gradle test を必須にしない。
