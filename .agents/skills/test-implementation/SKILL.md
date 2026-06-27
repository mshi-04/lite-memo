---
name: test-implementation
description: Lite Memo のテストを追加、修正、レビューするときに使う。JUnit 5、runTest、MockK、Turbine、ViewModel test、UseCase test、mapper test、Room/Compose instrumented test、AAA、観点 prefix を扱う作業で使用する。
---

# 目的

Lite Memo の変更した振る舞いを、既存の Unit Test / instrumented test 方針に沿って検証する。

# 最初に確認するもの

- [`docs/unit-test.md`](../../../docs/unit-test.md)
- [`docs/development-setup.md`](../../../docs/development-setup.md)
- 対象コードと対応する [`app/src/test/`](../../../app/src/test/) または [`app/src/androidTest/`](../../../app/src/androidTest/)
- 必要に応じて [`app/build.gradle.kts`](../../../app/build.gradle.kts)

# 手順

1. 変更した振る舞いが JVM Unit Test で検証できるか、androidTest が必要か判断する。
2. Domain の value object、UseCase、Repository interface 境界、mapper を優先してテストする。
3. suspend / Flow / Channel / debounce / coroutine を扱う場合は `runTest` と必要な test dispatcher を使う。
4. Flow や one-shot event の検証には必要に応じて Turbine を使う。
5. MockK の verify は呼び出し有無・回数・順序が主目的のときに使う。
6. テスト名は英語、観点 prefix、AAA コメント、原則 1 主要 assert を守る。
7. 変更範囲に応じて `testDevDebugUnitTest`、lint、androidTest など必要な検証を選ぶ。

# 注意事項

- 新規・更新する Unit Test では `runBlocking` を使わず `runTest` を使う。
- 複数の振る舞いを 1 テストに詰め込まない。
- Compose の細かい見た目より、状態遷移、表示分岐、ユーザー操作の結果を優先する。
- Room migration、DataStore、ContentResolver、Compose UI は必要に応じて instrumented test に置く。
