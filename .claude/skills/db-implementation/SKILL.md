---
name: db-implementation
description: Lite Memo の Room DB 周りを設計、実装、修正するときに使う。Room entity、DAO、LiteMemoDatabase、migration、app/schemas、Room testing、DB schema 変更を扱う作業で使用する。
---

# 目的

Lite Memo の Room schema と migration を、既存 DB 構成とテストに沿って安全に変更する。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md)
- [`docs/unit-test.md`](../../../docs/unit-test.md)
- [`app/src/main/kotlin/com/appvoyager/litememo/data/local/`](../../../app/src/main/kotlin/com/appvoyager/litememo/data/local/)
- [`app/schemas/`](../../../app/schemas/)
- [`app/src/androidTest/kotlin/com/appvoyager/litememo/data/local/`](../../../app/src/androidTest/kotlin/com/appvoyager/litememo/data/local/)

# 手順

1. 変更対象の entity、DAO、database version、migration、schema JSON を確認する。
2. schema を変える場合は entity / DAO / mapper / Repository / migration / schema export を一体で計画する。
3. `LiteMemoDatabase` の version を上げ、`LiteMemoMigrations.ALL` に migration を追加する。
4. migration は既存データの保持、default 値、index、外部キー、削除済みデータの扱いを明確にする。
5. DAO query は active / trash / search / calendar など既存の絞り込みと矛盾しないか確認する。
6. migration instrumented test と DAO test の要否を判断し、schema JSON の更新漏れを確認する。

# 注意事項

- Room schema 変更時は `app/schemas/` の差分を必ず確認する。
- アプリが未リリースでも、既存テストや schema export と矛盾する変更は避ける。
- DB 詳細を domain や UI に漏らさず、変換は mapper / Repository 実装に閉じる。
- DataStore や export/import の変更だけなら `data-implementation` を優先する。
