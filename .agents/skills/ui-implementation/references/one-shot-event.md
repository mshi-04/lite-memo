# one-shot event

Snackbar・画面遷移・認証要求など一回限りの通知を Channel event で扱う。

## 確認する対象

- 対象 ViewModel の event Channel と、Route 側の収集
- `docs/implementation-guidelines.md` の「UI Event / Error」節（Channel 種別の選択規約）

## 実装時の注意

- 画面上に残る失敗は UI state、一回限りの通知は Channel event に分ける。
- Channel 種別（`CONFLATED` / `BUFFERED` など）は docs の規約に従って選ぶ。規約本文はここで複製しない。
- 中間イベントを落とせない通知で latest-wins を仮定しない。

## テスト判断

- event の発行・順序・取りこぼしを Turbine などで検証する。

## 検証観点

- 画面回転・再収集で event を取りこぼさないか。
- state で表すべき状態を event に流していないか。
