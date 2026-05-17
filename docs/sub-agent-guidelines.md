# Sub-Agent Guidelines

Lite Memo でサブエージェントを使うときの運用方針です。
Clean Architecture / MVVM の境界、Unit Test 方針、レビュー観点を分けて確認するために使います。

## 基本方針

- サブエージェントは丸投げ先ではなく、視点を分けるために使う
- 親エージェントは最終判断、方針決定、差分統合に責任を持つ
- 実装とレビューを同じ視点だけで完結させない
- サブエージェントの結果は、親エージェントが根拠を確認してから採用する
- 軽微な修正では無理に使わない

## 使う場面

- `presentation` / `domain` / `data` にまたがる変更
- `presentation -> domain <- data` の依存方向確認
- ViewModel、UseCase、Repository interface の責務確認
- Room / DataStore / mapper の影響範囲調査
- 値オブジェクトや UseCase のテスト観点整理
- `runTest` が必要な Flow / suspend 処理の検証
- 実装後レビュー
- テスト・ビルド・CI 失敗調査
- PR 前の変更概要、確認事項、レビュー観点の整理

## 使わない場面

- typo 修正
- コメント修正
- 1 ファイル内の軽微修正
- 既存方針に沿った単純な文字列追加
- 使うことで確認コストが増えるだけの場合

## 推奨役割

- 調査担当: 既存実装、関連 docs、影響範囲を確認する
- 実装担当: 限定されたレイヤーや機能範囲を修正する
- レビュー担当: 設計、責務、依存方向、副作用を確認する
- テスト確認担当: Unit Test 方針、失敗原因、再現条件を確認する
- 要約担当: PR 用の変更概要、確認事項、レビュー観点を整理する

## Lite Memo での確認観点

- Domain 層が Android Framework に依存していないか
- UI が Repository implementation や data model に直接依存していないか
- ViewModel が UseCase 経由で domain にアクセスしているか
- data 層の変換責務が mapper に閉じているか
- 意味のある値が primitive のまま広がっていないか
- Unit Test が JUnit 5、英語名、1 主要 assert の方針に沿っているか
- suspend 関数や Flow のテストで `runTest` を使っているか
- レビュー指摘が `Critical` / `Suggestion` / `Nitpick` で整理できるか

## 運用ルール

- 依頼時は目的、対象ファイル、確認してほしい観点を明確にする
- 実装担当とレビュー担当は、可能な範囲で視点を分ける
- 親エージェントは、サブエージェントの出力をそのまま提出しない
- 親エージェントは、採用した内容と採用しなかった内容を整理する
- 最終的な修正内容、検証結果、未実施項目は親エージェントがまとめる
