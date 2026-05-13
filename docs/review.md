# Review

この文書は Lite Memo でコードレビューを行うときの形式をまとめます。

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
- 最後に Critical の件数を示し、マージ可否を明示する

例:

```
1. [Critical] `MemoRepository` の実装が domain 層に漏れている。data 層へ移動する。
2. [Suggestion] `saveMemo` は UseCase を経由せず Repository を直接呼んでいる。UseCase を挟むことを推奨。
3. [Nitpick] 変数名 `d` は意図が分かりにくい。`dueDate` などに変更を検討。
```

**Critical: 1件。マージ前に修正が必要。**
