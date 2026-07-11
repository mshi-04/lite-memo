---
name: ui-implementation
description: Lite Memo の UI 層の設計、実装、修正を扱う。Compose screen、Route、ViewModel、UI state、one-shot event、Navigation Compose、strings.xml、Preview、Compose UI Test が対象。
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

# 役割クラスと参照先

変更対象の役割クラスを見分け、該当する reference だけを読んでから実装する。
薄い役割は reference を作らず docs に委ねる。

| 役割クラス | 参照 |
| --- | --- |
| screen（`XxxRoute` / `XxxScreen`） | [`references/screen.md`](references/screen.md) |
| viewmodel | [`references/viewmodel.md`](references/viewmodel.md) |
| UI state / UI model | [`references/ui-state.md`](references/ui-state.md) |
| one-shot event | [`references/one-shot-event.md`](references/one-shot-event.md) |
| navigation | [`references/navigation.md`](references/navigation.md) |
| Compose UI Test | [`references/compose-ui-test.md`](references/compose-ui-test.md) |
| Preview / strings | reference なし。`docs/implementation-guidelines.md` の UI / Localization 方針に従う（`XxxRoute` の配線は screen 行を参照） |

# 手順

1. 変更対象の役割クラスを判定し、該当 reference を読む。
2. `ui -> domain <- data` の依存方向と、ViewModel 経由の domain アクセスを保つ。
3. reference の観点に沿って実装する。
4. 変更に応じて ViewModel test または Compose UI Test の要否を判断する。
5. Kotlin 変更は `./gradlew :app:ktlintCheck :app:detekt :app:testProdDebugUnitTest`、UI / resource 変更は `./gradlew :app:lintProdDebug`、Compose UI Test は `./gradlew :app:connectedDevDebugAndroidTest` で検証する。
6. 変更内容、実行した task と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- `ui` から data model、Room entity、Repository implementation を直接参照しない。
- UI 変更が Domain、Data、DB へ波及するときは、変更対象に対応する Skill を併用する。
- ViewModel に Android View や Context 依存を直接持ち込まない。
- 固定色の直書きを避け、Material 3 theme とライト / ダーク対応を優先する。
- Navigation 追加時は route 名、引数、戻る挙動を明確にする。
