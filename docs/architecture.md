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

## 想定レイヤー

- `presentation`: Compose UI、ViewModel、UI state
- `domain`: Entity、UseCase、Repository interface
- `data`: Repository implementation、Room、DataStore、DTO / mapper

パッケージ名や細かい分割は、実装が増えた時点で既存構成に合わせて決めます。
ただし、依存方向は `presentation -> domain <- data` を守ります。

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
- 例: メモ本文、メモタイトル、表示順、設定値など
- 値オブジェクトは生成時に制約を満たすようにし、不正な状態を後段へ流さない
- Room entity や DTO へ変換するときは、data 層で mapper を使う

## Data

- Room はメモなどの構造化データを扱う
- DataStore はテーマ、表示設定、初回起動状態などの軽量な設定値を扱う
- data 層の model と domain model がずれる場合は mapper を置く
