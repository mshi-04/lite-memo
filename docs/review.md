# Review

この文書は Lite Memo でコードレビューを行うときの形式をまとめます。

## 比較範囲

- ユーザーが base branch、commit、merge-base、対象ファイルを指定した場合は、その比較基準を使う
- 比較基準が未指定の場合は、現在の branch、追跡先、PR 情報から妥当な base を確認してから差分を読む

## 指摘の分類

指摘は以下の3段階で分類します。

| ラベル | 意味 |
|--------|------|
| **Critical** | マージ前に必ず直す。バグ、セキュリティ、アーキテクチャ違反など |
| **Suggestion** | 直すことを推奨。品質・保守性・一貫性に関わるが、マージをブロックするかは判断による |
| **Nitpick** | 好みや細部の改善。マージをブロックしない |

## 形式

- 指摘には上から通し番号を振る
- 各指摘に `[Critical]` / `[Suggestion]` / `[Nitpick]` のラベルを先頭に付ける
- 各指摘に対象ファイルと行番号、問題による影響、問題である理由、修正方針を含める
- 最後に Critical の件数を示し、マージ可否を明示する

例:

```markdown
1. [Critical] `app/src/main/.../Example.kt:20` — `MemoRepository` の実装が domain 層に漏れ、依存方向が逆転する。実装を data 層へ移動する。
2. [Suggestion] `app/src/main/.../ExampleViewModel.kt:35` — `saveMemo` が Repository を直接呼び、UI と data が結合している。UseCase を経由させる。
3. [Nitpick] `app/src/main/.../Example.kt:42` — 変数名 `d` から用途を判断できない。`dueDate` など意図を表す名前を検討する。
```

**Critical: 1件。マージ前に修正が必要。**
