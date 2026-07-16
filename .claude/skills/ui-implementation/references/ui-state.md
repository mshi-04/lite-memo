# UI state / UI model

画面状態を `XxxUiState`（`ui/state`）、表示用モデルを `XxxUiModel`（`ui/model`）に置く。

## 確認する対象

- `ui/state/` の `XxxUiState` と `ui/model/` の `XxxUiModel`、対応する domain model
- 表示変換を行う `component/DisplayStringExt` など

## 実装時の注意

- 画面状態は `ui/state` と ViewModel に寄せ、Compose に状態を持たせすぎない。
- domain model をそのまま UI へ流さず、表示に必要な形へ `XxxUiModel` で写す。
- loading / empty / error / content などの分岐を状態として表す。

## テスト判断

- 状態から表示分岐が決まる箇所は ViewModel test で押さえる。

## 検証観点

- UI が data model / Room entity を直接参照していないか。
- 表示に不要な domain 詳細を UI state に持ち込んでいないか。
