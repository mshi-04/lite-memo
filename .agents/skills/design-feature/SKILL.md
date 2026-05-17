---
name: design-feature
description: 実装前に既存仕様、影響範囲、責務分離、依存方向、公開 API の扱いを整理するときに使う。
---

# 目的

実装へ入る前に、Lite Memo の既存方針と現在のコードを確認し、最小限の変更範囲と設計上の判断を明確にする。

# 使う場面

- 新しい機能や既存機能の拡張を始める前。
- Domain / presentation / data の責務分担を決める必要があるとき。
- Repository interface、UseCase、公開 API の追加や変更が必要か判断するとき。

# 参照文書

- `docs/project-overview.md`
- `docs/tech-stack.md`
- `docs/architecture.md`
- `docs/implementation-guidelines.md`
- `docs/unit-test.md`

# 手順

1. 参照文書と対象コードを確認し、導入済みの仕組みと予定段階の仕組みを分ける。
2. 既存仕様、関連する画面、UseCase、Repository interface、data 実装、テストを洗い出す。
3. 変更がどのレイヤーに必要かを `presentation -> domain <- data` の依存方向で整理する。
4. 新しい公開 API が必要か、既存 contract で足りるかを判断する。
5. 値オブジェクト、Entity、UseCase、mapper のどこに責務を置くかを決める。
6. 実装範囲、影響範囲、UnitTest の要否、未決事項を短くまとめる。

# 注意事項

- 未導入の技術を実装済みとして扱わない。
- 既存 contract で足りる場合は、新しい API を増やさない。
- 詳細規約は docs 側を正とし、この Skill へ長く複製しない。
- 仕様が曖昧な場合は、実装を広げる前に前提を明確にする。
