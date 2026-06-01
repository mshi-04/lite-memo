---
name: implement-feature
description: 既存方針に沿って最小差分で機能を実装し、必要に応じて UnitTest を追加または修正するときに使う。
---

# 目的

Lite Memo の Clean Architecture + MVVM 方針に沿って、既存コードとの一貫性を保ちながら機能を実装する。

# 使う場面

- 承認済みの設計や実装方針に基づいてコードを変更するとき。
- Domain、data、ui のいずれかに機能を追加するとき。
- 変更に合わせて UnitTest を追加・修正するとき。

# 参照文書

- `docs/project-overview.md`
- `docs/tech-stack.md`
- `docs/architecture.md`
- `docs/implementation-guidelines.md`
- `docs/unit-test.md`
- `docs/development-setup.md`

# 手順

1. 対象機能の設計方針、影響範囲、既存コードの実装パターンを確認する。
2. 依存方向を守り、変更を必要なレイヤーに限定する。
3. Domain の意味を持つ値は、必要に応じて値オブジェクトや既存 model に寄せる。
4. data 層の詳細を ui 層へ漏らさず、必要なら mapper や Repository implementation に閉じる。
5. UI を変更する場合は Compose / Material 3、StateFlow、文字列リソース、ライト / ダーク対応を確認する。
6. 変更した振る舞いに対して、`docs/unit-test.md` に沿って UnitTest を追加・修正する。
7. 可能な範囲で UnitTest / build などの検証を実行し、未実行のものは理由を残す。

# 注意事項

- 承認済みのスコープから不要に広げない。
- 既存 docs と矛盾する独自ルールを追加しない。
- 新規 API、DB スキーマ、画面構成は必要性を確認してから増やす。
- テストを追加する場合は JUnit 5、英語名、1つの主要 assert を基本にする。
