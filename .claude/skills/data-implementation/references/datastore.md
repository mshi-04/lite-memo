# DataStore

テーマ・表示設定などの軽量な設定値を DataStore に閉じる。

## 確認する対象

- `data/repository/DataStoreUserSettingsRepository` と対応する domain interface
- 保存する key と既定値

## 実装時の注意

- 構造化データは Room に寄せ、DataStore は軽量設定に限定する。
- 既定値と未設定時の扱いを決め、Flow で観測する読み取りを崩さない。
- 設定値も意味を持つ場合は domain 側で型に寄せる。

## テスト判断

- 読み取り既定値と書き込み反映を検証する。Flow は `runTest` で扱う。

## 検証観点

- 設定追加時に既定値と後方互換が保たれているか。
