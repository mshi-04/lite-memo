# viewmodel

画面ごとの ViewModel で UI state を公開し、UseCase 経由で domain にアクセスする。

## 確認する対象

- `ui/viewmodel/` の対象 ViewModel と、公開する `XxxUiState`（StateFlow）
- 使用する UseCase と、one-shot event の Channel

## 実装時の注意

- UI state は StateFlow で公開し、UseCase 経由で domain にアクセスする。
- Android View / Context 依存を ViewModel に直接持ち込まない。
- 画面上に残る失敗は UI state、一回限りの通知は one-shot event で扱う（`one-shot-event.md`）。
- 画面固有の状態とドメインモデルを混ぜすぎない。

## テスト判断

- 状態遷移・分岐・event 発行を ViewModel test で押さえる。`runTest` と必要な test dispatcher を使う。

## 検証観点

- 初期値・再収集で状態が破綻しないか。
- UseCase の失敗時に UI state が適切に反映されるか。
