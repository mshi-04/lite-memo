# provider

時刻・ID 生成などの外部依存を、domain には抽象だけ置き、実装は data 層へ閉じる。

## 確認する対象

- `domain/provider/` の既存抽象（`CurrentTimeProvider`、`MemoIdProvider`、`TagIdProvider`、`MemoImageIdProvider`）
- `data/provider/` の対応実装（`System...` / `Uuid...`）と Hilt binding

## 実装時の注意

- domain 側は interface のみとし、Android 依存や具体実装を持ち込まない。
- 既存 provider で足りるか先に確認し、意味が重複する抽象を増やさない。
- 実装を追加する場合は data 層で binding を用意する（`data-implementation`）。

## テスト判断

- provider を差し替えて、時刻・ID に依存する UseCase を決定的にテストする。

## 検証観点

- domain から実装詳細が漏れていないか。
- テストで固定値へ差し替えられる形になっているか。
