# Project Overview

この文書は Lite Memo の概要と、AI エージェントが最初に確認すべき場所をまとめます。

## 概要

Lite Memo は Android 向けの軽量メモアプリです。
小さく始め、あとから永続化、設定、広告、CI、公開フローを足していく前提です。

現在の基本情報:

- パッケージ名: `com.appvoyager.litememo`
- ルートプロジェクト名: `Lite Memo`
- メインモジュール: `:app`

## 主な確認先

- `app/`: Android アプリ本体
- `app/src/main/java/com/appvoyager/litememo/`: Kotlin ソース
- `app/src/main/java/com/appvoyager/litememo/ui/theme/`: Compose / Material 3 テーマ
- `app/src/main/res/`: Android リソース
- `gradle/libs.versions.toml`: 依存関係とプラグインのバージョン管理
- `.github/workflows/`: GitHub Actions を追加する場所

## 判断の前提

- まず Gradle とソースを見て、現在導入済みのものを確認する
- 予定技術を、実装済みの仕組みとして扱わない
- 既存構成を優先し、必要以上に大きな再編をしない
- 構造方針は Clean Architecture + MVVM とし、詳細は `docs/architecture.md` を確認する
