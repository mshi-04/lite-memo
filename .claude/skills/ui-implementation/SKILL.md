---
name: ui-implementation
description: Lite Memo の UI 層を設計、実装、修正するときに使う。Compose screen、Route、ViewModel、UI state、one-shot event、Navigation Compose、strings.xml、Preview、Compose UI Test を扱う作業で使用する。
---

# 目的

Lite Memo の UI 層を、既存の Compose / MVVM 構成に沿って変更する。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md)
- [`docs/unit-test.md`](../../../docs/unit-test.md)
- [`app/src/main/kotlin/com/appvoyager/litememo/ui/`](../../../app/src/main/kotlin/com/appvoyager/litememo/ui/)
- [`app/src/main/res/values/strings.xml`](../../../app/src/main/res/values/strings.xml)
- 必要に応じて [`app/src/androidTest/kotlin/com/appvoyager/litememo/ui/`](../../../app/src/androidTest/kotlin/com/appvoyager/litememo/ui/)

# 手順

1. 対象画面の `XxxRoute`、`XxxScreen`、`XxxViewModel`、`XxxUiState`、表示用 model を確認する。
2. UI は ViewModel にイベントを渡し、ViewModel は UseCase 経由で domain にアクセスする形を守る。
3. Compose は状態を持ちすぎず、画面状態は `ui/state` と ViewModel に寄せる。
4. 画面上に残る失敗は UI state、一回限りの Snackbar・遷移・認証要求は Channel event で扱う。
5. 表示文字列は `strings.xml` / `values-en/strings.xml` に寄せ、長い英語文字列でも崩れない余白を確認する。
6. Preview がある画面は壊さず、必要なら preview state を更新する。
7. UI 変更に応じて ViewModel test または Compose UI Test の要否を判断する。

# 注意事項

- `ui` から data model、Room entity、Repository implementation を直接参照しない。
- ViewModel に Android View や Context 依存を直接持ち込まない。
- 固定色の直書きを避け、Material 3 theme とライト / ダーク対応を優先する。
- Navigation 追加時は route 名、引数、戻る挙動を明確にする。
