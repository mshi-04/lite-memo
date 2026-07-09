# migration

`LiteMemoDatabase` の version を上げ、`LiteMemoMigrations.ALL` に migration を追加する。

## 確認する対象

- `data/local/LiteMemoDatabase`（version）と `data/local/migration/LiteMemoMigrations`
- 変更前後の `app/schemas/` JSON

## 実装時の注意

- version を上げ、`ALL` に追加する。追加漏れは起動時 crash になる。
- 既存データの保持、default 値、index、外部キー、削除済みデータの扱いを明示する。
- 破壊的 migration（fallback）に安易に頼らない。

## テスト判断

- migration instrumented test を追加し、旧 schema からの移行を検証する。

## 検証観点

- 旧バージョンからの移行でデータが失われないか。
- schema export と migration 結果が一致するか。
