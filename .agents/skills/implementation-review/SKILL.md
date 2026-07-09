---
name: implementation-review
description: Lite Memo の実装差分をレビューするときに使う。依存方向、責務分離、過剰 API、DB migration、UI state、data mapper、Unit Test 不足、レビュー指摘形式を確認する作業で使用する。
---

# 目的

Lite Memo の差分が既存方針に沿っているかを確認し、マージ前に直すべき問題を具体的に洗い出す。

# 最初に確認するもの

- [`docs/review.md`](docs/review.md)
- [`docs/architecture.md`](docs/architecture.md)
- [`docs/implementation-guidelines.md`](docs/implementation-guidelines.md)
- [`docs/unit-test.md`](docs/unit-test.md)
- 差分と関連コード

# 手順

1. 差分の目的、変更範囲、既存実装との関係を確認する。
2. `ui -> domain <- data` の依存方向が崩れていないか見る。
3. ViewModel、UseCase、Repository interface、Repository implementation、mapper、DAO の責務が混ざっていないか見る。
4. 新規 API、抽象化、DB schema、画面構成が本当に必要か確認する。
5. 変更した振る舞いに対するテストがあるか、テスト観点が妥当か確認する。
6. 指摘は `Critical` / `Suggestion` / `Nitpick` に分類し、修正すべき順に書く。

# 注意事項

- 好みではなく、バグ、依存方向、責務分離、保守性、テスト不足を優先する。
- 問題がない場合も、残るリスクや未検証項目を明記する。
- DB schema 変更がある場合は migration と schema JSON を必ず確認する。
- UI 変更がある場合は strings、preview、ライト / ダーク、イベント取りこぼしを確認する。
