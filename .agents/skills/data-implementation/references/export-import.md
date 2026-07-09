# export / import

JSON export / import でメモ・タグの構造化データを入出力する。

## 確認する対象

- `data/export/`（`ExportFileReader` / `ExportFileWriter`）、`data/model/export/` の DTO、`ExportDataMapper`
- `ExportMemosUseCase` / `ImportMemosUseCase` と domain の `ExportData`

## 実装時の注意

- 対象はメモ・タグの構造化データに限り、画像ファイルは対象外とする。
- import で同一 ID を上書きすると既存の添付画像は失われる前提で扱う。
- 入出力フォーマットの後方互換（欠損・追加フィールド）を壊さない。

## テスト判断

- DTO ↔ domain 変換と、往復（export → import）の一致を JVM Unit Test で押さえる。

## 検証観点

- 破損・欠損フィールドの入力で無効なデータを作らないか。
- 画像を対象外とする前提が実装と一致しているか。
