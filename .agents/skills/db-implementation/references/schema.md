# schema (app/schemas)

Room の schema JSON export を entity / migration と一致させる。

## 確認する対象

- `app/schemas/` 配下の version 別 JSON
- 対応する entity と database version

## 実装時の注意

- schema 変更時は export JSON を必ず更新し、差分を同じ commit に含める。
- version 番号と export ファイルの対応を崩さない。
- 未リリースでも、既存 test / export と矛盾する変更は避ける。

## テスト判断

- schema 変更に伴う migration / DAO test の要否を判断する。

## 検証観点

- 追加された JSON が entity の実体と一致するか。
- export 漏れ（未生成の version）がないか。
