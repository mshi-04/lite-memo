# Claude Code

この文書は Claude Code 固有の運用（skill 同期 / CLAUDE.md import）を扱う。

## Skill の正本と同期

複数の AI ツールで同じ skill を使うため、正本は `.agents/skills/` に置く。
Claude Code は `.claude/skills/<skill-name>/SKILL.md` を検出するため、この directory は
`.agents/skills/` から生成する。`.claude/skills/` 配下の同期対象 skill は直接編集しない。

`.agents/skills/<skill-name>/agents/` 配下（Codex 専用の `openai.yaml` など）は同期対象外で、
`.claude/skills/` には生成されない。

```sh
./scripts/sync-claude-skills.ps1
./scripts/sync-claude-skills.ps1 -Check
```

skill を追加・変更・削除した場合は、同期後に生成された `.claude/skills/` の差分も同じ commit に含める。
CI は `-Check` を実行し、未同期または削除漏れを検出する。

`-Check` は `AGENTS.md` の skill 一覧が `.agents/skills/` と一致するかも検証する。
skill を増減したら AGENTS.md の一覧も更新する。AGENTS.md の説明文は手書きのため自動生成されない。

`.claude/skills/` は同期対象であり、手作業の skill を置かない。
Claude Code 固有の手作業 skill を追加する場合は、`scripts/sync-claude-skills.ps1` の
`$excludedClaudeSkillNames` へ名前を加えて同期対象から除外する。

## Claude Code の共通指示

`CLAUDE.md` は `@AGENTS.md` を import する。共通指示は `AGENTS.md` に集約し、
Claude Code 固有の指示だけを `CLAUDE.md` に追加する。

`@path` import は `CLAUDE.md` の機能であり、`SKILL.md` から `.agents/skills/` を
import する仕組みには使わない。`.claude/skills/` には正本の内容を実体コピーとして生成する。
