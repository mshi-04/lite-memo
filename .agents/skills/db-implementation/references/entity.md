# entity

Room entity を `data/local/entity` に定義し、schema export と整合させる。

## 確認する対象

- `data/local/entity/` の既存 entity（`MemoEntity`、`TagEntity`、`MemoTagRefEntity`、`MemoImageEntity`）
- 対応する mapper と `app/schemas/` の JSON

## 実装時の注意

- カラム追加・変更は既定値、null 許容、index、外部キーを明確にする。
- entity 変更は mapper / migration / schema export と一体で計画する。
- DB 詳細を domain / UI に漏らさない。

## テスト判断

- entity 変更に伴う DAO / migration の test 要否を判断する。

## 検証観点

- `app/schemas/` の差分が entity 変更と一致するか。
- 既存データとの後方互換が保てるか。
