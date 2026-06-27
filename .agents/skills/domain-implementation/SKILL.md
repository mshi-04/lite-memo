---
name: domain-implementation
description: Lite Memo の Domain 層を設計、実装、修正するときに使う。domain model、value object、UseCase、Repository interface、provider、Android 非依存のビジネスルールを扱う作業で使用する。
---

# 目的

Lite Memo のビジネスルールと公開 contract を、Android Framework に依存しない domain 層へ閉じる。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md)
- [`docs/unit-test.md`](../../../docs/unit-test.md)
- [`app/src/main/kotlin/com/appvoyager/litememo/domain/`](../../../app/src/main/kotlin/com/appvoyager/litememo/domain/)
- 関連する [`app/src/test/kotlin/com/appvoyager/litememo/domain/`](../../../app/src/test/kotlin/com/appvoyager/litememo/domain/)

# 手順

1. 対象の model、value object、UseCase、Repository interface、provider を確認する。
2. 既存 contract で足りるか、新しい Repository interface / UseCase / provider が必要か判断する。
3. Domain の意味を持つ値は primitive のまま広げず、必要に応じて value object に寄せる。
4. UseCase は 1 つの明確な操作を表し、validation と business rule を UI / data へ漏らさない。
5. 外部依存が必要な場合は provider または Repository interface として domain に抽象だけを置く。
6. 変更した rule は JVM Unit Test で押さえる。

# 注意事項

- Domain 層に Context、URI、Room、DataStore、Compose、Android resource を持ち込まない。
- Repository implementation の都合で domain model を歪めない。
- 新しい公開 API は、既存 contract で足りない理由がある場合だけ追加する。
- 例外や require/check の意味が UI 表示と混ざらないようにする。
