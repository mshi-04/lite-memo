# mapper

data model ↔ domain model の変換責務を mapper に閉じる。

## 確認する対象

- `data/mapper/` の既存 mapper（`MemoMapper`、`TagMapper`、`ExportDataMapper`）
- 変換元の entity / DTO と、変換先の domain model / value object

## 実装時の注意

- domain model の不変条件を壊す値を流さない（value object の生成制約を尊重する）。
- 変換ロジックを Repository 実装や DAO に散らさず mapper に集約する。
- null / 既定値 / 欠損フィールドの扱いを明示する。

## テスト判断

- 代表値・境界・欠損を含む変換往復を JVM Unit Test で押さえる。

## 検証観点

- entity 追加・schema 変更時に変換漏れがないか。
- 双方向変換で情報が失われていないか。
