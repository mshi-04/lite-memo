---
name: agent-docs-maintenance
description: Lite Memo の AI 向け文書、AGENTS.md、CLAUDE.md、docs、.agents/skills、.claude/skills、skill 同期運用を追加または更新するときに使う。
---

# 目的

Lite Memo の AI 作業導線を、薄い入口文書と詳細 docs / skills の分担を保って更新する。

# 最初に確認するもの

- [`AGENTS.md`](AGENTS.md)
- [`CLAUDE.md`](CLAUDE.md)
- [`docs/claude-code.md`](docs/claude-code.md)
- [`docs/project-overview.md`](docs/project-overview.md)
- [`docs/architecture.md`](docs/architecture.md)
- [`docs/implementation-guidelines.md`](docs/implementation-guidelines.md)
- [`.agents/skills/`](..)
- [`scripts/sync-claude-skills.ps1`](scripts/sync-claude-skills.ps1)

# 手順

1. 入口に置く情報と、docs / skill に分ける情報を整理する。
2. `.agents/skills/` を正本として編集する（`SKILL.md` と `references/`）。Codex 固有設定は各 skill の `agents/openai.yaml` に置く。
3. skill を追加・削除した場合は `AGENTS.md` の skill 一覧も更新する。
4. `./scripts/sync-claude-skills.ps1` を実行して `.claude/skills/` を生成する。
5. `./scripts/sync-claude-skills.ps1 -Check` を実行し、exit 0 を確認する。
6. 変更内容・同期結果・未確認事項を簡潔に報告する。

# 注意事項

- `.agents/skills/` が Skill の正本。`.claude/skills/` は同期生成物であり直接編集しない。
- 共通 `SKILL.md` は AI エージェント共通の作業手順として書き、Claude Code / Codex いずれかのツール固有設定を混ぜない。
- Codex 固有設定は `agents/openai.yaml` に分離する。`agents/` 配下は Claude 同期対象外で `.claude/skills/` には生成されない。
- PowerShell script が実行できない環境では同期・`-Check` が実行できない旨と理由を報告し、`.claude/skills/` を手で編集しない。
- `AGENTS.md` は地図として薄く保ち、詳細手順は docs または `.agents/skills/` に置く。
- marker file や二重管理の仕組みを増やさない。未導入の技術を実装済みとして書かない。
- アプリ実装ファイルを変更しない docs 作業では、Gradle test を必須にしない。
