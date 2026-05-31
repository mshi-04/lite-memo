# Project Overview

この文書は Lite Memo の概要と、AI エージェントが最初に確認すべき場所をまとめます。

## 概要

Lite Memo は Android 向けの軽量メモアプリです。
永続化（Room）、設定（DataStore）、CI（GitHub Actions）は導入済みで、残る主な予定は広告（AdMob）と Google Play 公開フローです。

現在の基本情報:

- パッケージ名: `com.appvoyager.litememo`
- ルートプロジェクト名: `Lite Memo`
- メインモジュール: `:app`
- ビルドフレーバー: `dev` / `prod`

## 主な確認先

- `app/`: Android アプリ本体
- `app/src/main/kotlin/com/appvoyager/litememo/`: Kotlin ソース
  - `ui/`: Compose 画面、ViewModel、UI state、ナビゲーション、テーマ
  - `domain/`: model、値オブジェクト、UseCase、Repository interface、provider
  - `data/`: Repository 実装、Room、DataStore、mapper、Hilt module、Export/Import
- `app/src/main/res/`: Android リソース（表示文字列は `strings.xml` に集約）
- `app/schemas/`: Room のエクスポート済みスキーマ
- `gradle/libs.versions.toml`: 依存関係とプラグインのバージョン管理
- `.github/workflows/`: CI / CodeQL（設定済み）
- `fastlane/`: KtLint・テスト実行などの CI レーン

## 判断の前提

- まず Gradle とソースを見て、現在導入済みのものを確認する
- 予定技術（AdMob、Google Play 公開）を、実装済みの仕組みとして扱わない
- 既存構成を優先し、必要以上に大きな再編をしない
- 構造方針は Clean Architecture + MVVM とし、詳細は `docs/architecture.md` を確認する
