---
name: data-implementation
description: Lite Memo の Data 層を設計、実装、修正するときに使う。Repository 実装、mapper、DataStore、export/import、ContentResolver、Hilt module、provider 実装、domain と data の変換境界を扱う作業で使用する。
---

# 目的

Lite Memo の data 層で、domain の interface を既存データ源に接続し、実装詳細を UI / domain へ漏らさない。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md)
- [`docs/unit-test.md`](../../../docs/unit-test.md)
- [`app/src/main/kotlin/com/appvoyager/litememo/data/`](../../../app/src/main/kotlin/com/appvoyager/litememo/data/)
- 関連する [`app/src/test/kotlin/com/appvoyager/litememo/data/`](../../../app/src/test/kotlin/com/appvoyager/litememo/data/) と [`app/src/androidTest/kotlin/com/appvoyager/litememo/data/`](../../../app/src/androidTest/kotlin/com/appvoyager/litememo/data/)

# 手順

1. 対象の Repository implementation、mapper、DataStore、export/import、Hilt module を確認する。
2. domain interface の期待値と data source の制約を分けて読む。
3. data model と domain model がずれる場合は mapper に変換責務を閉じる。
4. Android 依存の URI、ContentResolver、Preferences、外部 SDK は data 層または app entry 側に閉じる。
5. transaction、重複、順序、失敗時の部分更新の扱いを Repository 実装で明確にする。
6. mapper と Repository 実装の振る舞いは JVM Unit Test または androidTest で押さえる。

# 注意事項

- data 層から UI state や Compose model を参照しない。
- domain model の不変条件を壊す値を mapper から流さない。
- Room schema 変更を伴う場合は `db-implementation` も併用する。
- DataStore は軽量な設定用とし、構造化データは Room 側に寄せる。
