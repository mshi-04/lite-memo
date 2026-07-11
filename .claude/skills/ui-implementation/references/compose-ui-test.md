# Compose UI Test

画面の表示分岐・ユーザー操作の結果を androidTest で確認する。

## 確認する対象

- `app/src/androidTest/` の既存 Compose UI Test と testTag（`MemoCardTestTags`、`MemoEditTestTags` など）
- `docs/unit-test.md` のテスト方針

## 実装時の注意

- 細かい見た目より、状態遷移・表示分岐・操作結果を優先して検証する。
- testTag を安定した識別子として使い、文言依存の脆いアサートを避ける。
- ViewModel ロジックで検証できるものは JVM 側に寄せ、UI Test を過剰に増やさない。

## テスト判断

- Compose UI に固有の分岐・操作結果だけを instrumented test に置く。

## 検証観点

- loading / empty / error / content の表示が確認できるか。
- 主要操作（作成・保存・削除・選択）の結果が反映されるか。
