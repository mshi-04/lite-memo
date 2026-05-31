---
name: review-implementation
description: 実装後に設計意図、責務分離、依存方向、UnitTest、過剰実装を確認するときに使う。
---

# 目的

実装が Lite Memo の既存方針に沿っているかを確認し、マージ前に直すべき問題を具体的に洗い出す。

# 使う場面

- 実装完了後の自己レビューやコードレビュー。
- 依存方向、レイヤー責務、UnitTest の不足を確認したいとき。
- 過剰実装や不要な API 追加がないか確認したいとき。

# 参照文書

- `docs/architecture.md`
- `docs/implementation-guidelines.md`
- `docs/unit-test.md`
- `docs/review.md`

# 手順

1. 差分と関連コードを確認し、実装意図と実際の変更内容が一致しているか見る。
2. `ui -> domain <- data` の依存方向が崩れていないか確認する。
3. ViewModel、UseCase、Repository interface、data 実装、mapper の責務が混ざっていないか確認する。
4. 新規 API や抽象化が本当に必要か、既存 contract で足りなかったかを確認する。
5. UnitTest が変更した振る舞いを押さえているか、`docs/unit-test.md` と整合しているか確認する。
6. 指摘がある場合は `docs/review.md` の分類に沿って、修正すべき順にまとめる。

# 注意事項

- 好みの差ではなく、バグ、依存方向、責務分離、保守性、テスト不足を優先する。
- docs の規約本文をここへ複製せず、必要な文書を参照する。
- 過剰実装を見つけた場合は、削る理由と安全な最小修正を示す。
- 問題がない場合も、残るリスクや未検証項目は明記する。
