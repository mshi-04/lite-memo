# Architecture

Lite Memo は Clean Architecture をベースに、UI 層は MVVM で構成します。
実装時は過剰に層を増やさず、機能が増えた時に自然に拡張できる粒度を優先します。

## 基本方針

- UI は ViewModel に依存する
- ViewModel は UseCase に依存する
- UseCase は Repository interface に依存する
- Repository implementation は Room、DataStore、外部 SDK などのデータ源を扱う
- Domain 層は Android Framework に依存しない
- 依存注入は Hilt で行う

## レイヤー構成

プレゼンテーション層は `ui` パッケージとして実装します。依存方向は `ui -> domain <- data` を守ります。

- `ui`: Compose 画面、ViewModel、UI state
  - `screen`: 状態を持たない `XxxScreen` と、ViewModel と接続する `XxxRoute` に分ける
  - `viewmodel`: 画面ごとの ViewModel
  - `state`: UI state（`XxxUiState`）と表示用モデル（`XxxUiModel`）
  - `component`: 再利用する Compose 部品
  - `navigation`: 画面遷移先の定義
  - `theme`: Compose / Material 3 テーマ
- `domain`: Android Framework に依存しないビジネスロジック
  - `model` / `model/value`: ドメインモデルと値オブジェクト
  - `usecase`: 1 操作につき 1 UseCase
  - `repository`: Repository interface
  - `provider`: 時刻・ID 生成など外部依存の抽象（例: `CurrentTimeProvider`、`MemoIdProvider`）
- `data`: domain の interface を実装し、データ源を扱う
  - `local`: Room（`database` / `entity` / `dao` / `migration`）
  - `repository`: Repository 実装（例: `RoomMemoRepository`、`DataStoreUserSettingsRepository`）
  - `mapper`: data model ↔ domain model 変換
  - `di`: Hilt module
  - `export`: Export / Import の入出力

新しいファイルは、上記の既存パッケージ構成に合わせて追加します。

## MVVM

- ViewModel は UI state を StateFlow で公開する
- Compose UI は StateFlow を収集し、イベントを ViewModel に渡す
- ViewModel に Android View や Context 依存を直接持ち込まない
- 画面固有の状態とドメインモデルを混ぜすぎない

## UseCase

- 画面から直接 Repository implementation を呼ばない
- 1つの UseCase は、できるだけ1つの明確な操作を表す
- 単純な CRUD でも、UI とデータ層の結合を避ける必要がある場合は UseCase を挟む

## Value Object

- Domain の意味を持つ値は、必要に応じて値オブジェクトで表す
- 例: `MemoTitle`、`MemoId`、`TagName`、`TagColor`、`TimestampMillis` など
- 値オブジェクトは生成時に制約を満たすようにし、不正な状態を後段へ流さない
- Room entity や DTO へ変換するときは、data 層で mapper を使う

## Data

- Room はメモ・タグなどの構造化データを扱う。スキーマは `app/schemas/` にエクスポートし、変更時は migration を追加する
- DataStore はテーマ、表示設定などの軽量な設定値を扱う
- 時刻・ID 生成や外部 SDK は domain の provider / repository interface 経由で扱い、実装は data 層に閉じる
- data 層の model と domain model がずれる場合は mapper を置く
