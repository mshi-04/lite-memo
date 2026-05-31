# Implementation Guidelines

この文書は実装時の基本方針をまとめます。
詳細設計は、必要になった時点で別文書またはコード上で明確にしてください。

## UI

- UI は Jetpack Compose と Material 3 を基本にする
- 下部主要導線は Material 3 の `NavigationBar` を使う
- Compose Preview を壊さないように、UI コンポーネントは小さく分ける
- ライト / ダーク両対応を前提にし、固定色の直書きを避ける

## Architecture

- 構造は Clean Architecture + MVVM を基本にする
- 依存方向は `ui -> domain <- data` を守る（プレゼンテーション層は `ui` パッケージ）
- UI は ViewModel に依存し、ViewModel は UseCase 経由で domain にアクセスする
- data 層の実装詳細を `ui` 層へ漏らさない
- DI は Hilt を使う
- 詳細なレイヤー / パッケージ構成は `docs/architecture.md` を参照する

## Kotlin

- 関数名は英語にする
- ガード条件は早期リターンで扱い、ネストを深くしない
- 意味のある値は primitive のまま広げず、必要に応じて値オブジェクトにする
- 値オブジェクトは不正な値を作れない形に寄せる

## Navigation

- 画面遷移は Navigation Compose に寄せる
- 画面追加時は route 名、引数、戻る挙動を明確にする

## State / Data

- ViewModel から UI へは StateFlow を中心に状態を公開する
- 非同期処理は Coroutines を使い、UI スレッドをブロックしない
- 構造化データは Room、軽量な設定値は DataStore に分ける

## UI Event / Error

- 画面上に状態として残る失敗は UI state の boolean や sealed state で表す
- Snackbar、画面遷移、認証要求など一回限りの通知は Channel event で表す
- 一回限り event の Channel は、`latest wins`（最新だけ届けばよい）が成立する場合に限り `Channel.CONFLATED` を使う
- 中間イベントを落とせない通知（内容の異なる Snackbar、結果付き画面遷移など）は `Channel.BUFFERED` など取りこぼさない手段を選ぶ

## Test

- Unit Test の方針は `docs/unit-test.md` を確認する

## Localization

- 表示文字列は `strings.xml` に寄せる
- 日本語 / 英語対応を前提にする
- UI 実装時は長い英語文字列でも崩れない余白を確保する

## まだ固定しないこと

以下は未確定で、必要になった時点で決めます。

- 広告（AdMob）の表示位置や頻度
- Google Play 公開手順

DB スキーマ、パッケージ分割、CI（GitHub Actions）のジョブ構成は既に確立しているため、変更時は既存構成に合わせます（スキーマ変更時は Room の migration を追加する）。
