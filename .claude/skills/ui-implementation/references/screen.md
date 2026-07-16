# screen

状態を持たない `XxxScreen`（`ui/screen`）と、ViewModel と接続する `XxxRoute`（`ui/route`）に分ける。

## 確認する対象

- `ui/route/` の `XxxRoute` と `ui/screen/` の `XxxScreen` ペア、対応する `XxxUiState`
- 再利用する `ui/component/` 部品と testTag

## 実装時の注意

- `XxxScreen` は状態を持たず、状態とイベントハンドラを引数で受ける。
- `XxxRoute` で ViewModel を collect し、event を screen へ渡す。
- 固定色を直書きせず、Material 3 theme とライト / ダーク対応を保つ。
- Preview を壊さない（Route の配線や Preview の細部は docs に委ねる）。

## テスト判断

- 表示分岐・操作結果を確認する Compose UI Test の要否を判断する（`compose-ui-test.md`）。

## 検証観点

- loading / empty / error / content の表示分岐が揃っているか。
- 長い英語文字列でも崩れない余白があるか。
