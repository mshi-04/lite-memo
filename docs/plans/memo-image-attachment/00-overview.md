# メモ画像添付機能 実装指示書 — 全体概要

Lite Memo にメモへの画像添付機能を追加するための実装指示書。
この文書群だけで(事前の会話文脈なしで)実装できるよう自己完結に書いてある。
実装前に必ず本書(00)を読み、その後、担当ブランチの各論(01〜03)に進むこと。

- [`01-domain.md`](01-domain.md): ブランチ `feature/memo-image-domain`(Domain 層)
- [`02-data.md`](02-data.md): ブランチ `feature/memo-image-data`(Data 層)
- [`03-ui.md`](03-ui.md): ブランチ `feature/memo-image-ui`(UI 層)

## 1. 機能要件

- メモ編集画面から画像を添付できる(Android Photo Picker を使用)
- 1メモに **複数枚**、**順序付き** で添付できる
  - アプリ独自の保存枚数上限は設けない
  - 1回の選択数は Android Photo Picker / fallback 先の仕様に従う
- 対応形式は Photo Picker が返すものをそのまま受け入れる(jpeg / png / webp / gif 等。再圧縮しない)
- メモ一覧(ホーム)とカレンダーのメモカードに **先頭画像のサムネイル** を表示する
  - 画像ファイルの読み込みに失敗した場合のみアイコンにフォールバック
- 編集画面で添付済み画像の削除(1枚単位)ができる
- **エクスポート/インポート(JSON v1)は画像を対象外とする**。既存フォーマットは一切変更しない
  - 制約として「エクスポートに画像は含まれない」「インポートで既存メモを上書きすると添付画像は失われる」を仕様とする
- **アプリは未リリースのため Room マイグレーションは作らない**(DB version 1 のままスキーマを直接変更する)

## 2. 対象リポジトリの前提(必読)

- パッケージ: `com.appvoyager.litememo`、単一モジュール `:app`
- Clean Architecture + MVVM。依存方向は **`ui -> domain <- data`**(domain は何にも依存しない)
  - domain に Android 依存(`Context` / `Uri` / `File` / Room / Compose)を持ち込むことは禁止
  - ViewModel に `Context` を持ち込むことも禁止(Activity Result ランチャーは Route/Screen 側)
  - UI から Repository 実装を直接呼ばない(必ず UseCase 経由)
- UI は画面ごとに quartet 構成: `XxxRoute`(Hilt + collect)→ `XxxScreen`(stateless)→ `XxxViewModel`(StateFlow + Channel イベント)→ `XxxUiState` + 表示モデル(`MemoUiModel` 等)
- 詳細規約は各ドキュメント/Skill を参照:
  - `docs/architecture.md` / `docs/implementation-guidelines.md` / `docs/unit-test.md`
  - `.agents/skills/domain-implementation/SKILL.md`、`data-implementation`、`db-implementation`、`ui-implementation`、`test-implementation`

### テスト規約(全ブランチ共通)

- JVM unit test は **JUnit 5(Jupiter)+ MockK + Turbine**、コルーチンは **`runTest` のみ**(`runBlocking` 禁止)
- テストメソッド名は **英語 camelCase + 観点 prefix**(`Normal` / `Boundary` / `Error` / `Interaction` / `Flow` / `Coroutine` / `StateTransition`)
  - 例: `normalAttachImagesAddsImagesToUiState`、`errorInvokeThrowsWhenFileNameContainsPathSeparator`
- 本文は `// Arrange` / `// Act` / `// Assert` コメント必須。`// Act` 直下に `// 観点: <意図>` を書く。主要 assert は1テスト1つ
- androidTest(instrumented)は **JUnit 4**(`org.junit.Test`、`AndroidJUnit4`)である点に注意
- Room / ContentResolver / 実ファイル I/O / Compose UI は instrumented test、その他は JVM test

### CI・静的解析(各ブランチで green 必須)

```sh
./gradlew app:ktlintCheck app:detekt :app:lintProdDebug :app:testProdDebugUnitTest
# instrumented(エミュレータ必要):
./gradlew :app:connectedDevDebugAndroidTest
# fastlane がある環境なら: bundle exec fastlane android ci
```

- Android Lint は `warningsAsErrors = true`。**strings を ja/en どちらか片方にしか足さないと即 fail する**
- detekt は `config/detekt/baseline.xml` を超える新規違反で fail。既存の `@Suppress("LongParameterList")` の範囲で吸収する
- Kover のカバレッジ集計は `app/build.gradle.kts` の `kover { classes(...) }` allowlist に載っているパッケージのみ対象。
  **本機能は allowlist を変更しなくて済むようクラス配置を設計してある**(`data.image` は対象外パッケージに置く)。配置を崩さないこと

### Git 運用

- ブランチは `feature/<kebab-case>`、PR は **`develop` ブランチ向け**
- 3ブランチはスタック構成(下記)。前段がマージされたら次を rebase して PR
- コミットメッセージは日本語命令形(例: 「メモ画像添付のドメイン基盤を追加する」)

## 3. アーキテクチャ決定(全ブランチ共通の前提)

### 決定1: 画像ファイルはアプリ内部ストレージにコピーして保持する

Photo Picker が返す URI の読み取り許可は一時的なので、**選択された時点で即座に**
`context.filesDir/memo_images/<imageId>.<拡張子>` へコピーする。
拡張子は `ContentResolver.getType()` + `MimeTypeMap` で MIME から決定し、解決できなければ `img` とする。
再圧縮・別サムネイルファイルの生成はしない(一覧表示は Coil がレイアウトサイズに合わせてサブサンプルデコードするためメモリ安全)。

### 決定2: ドメインには文字列 value object だけを通す

`File` / `Uri` を domain に漏らさないため、既存の `ExportFileReference`(絶対URI文字列の VO)の先例に従う。

- 入力: `ImageSourceReference`(Picker の URI 文字列を包む VO)
- 保存結果: `MemoImage(id: MemoImageId, fileName: MemoImageFileName)`(ファイル名のみ。ディレクトリの知識は data 層に閉じる)
- 表示: `MemoImageStore.resolveImagePath(fileName): String`(絶対パス文字列)を UseCase 経由で UI に渡し、UI が `java.io.File(path)` にして Coil に食わせる

ファイル操作の抽象は `domain/repository/MemoImageStore` インターフェース(3メソッド)。実装は data 層で2分割:

- `data/image/MemoImageFileDataSource` — ContentResolver / File I/O の生実装(instrumented test 対象、Kover 対象外)
- `data/repository/FileSystemMemoImageStore` — ID採番・ファイル名組み立て・委譲(MockK で JVM test 可能、Kover 対象)

### 決定3: 画像の永続化は既存の自動保存(autosave)フローに載せる

このアプリの保存は保存ボタンではなく **1000ms デバウンスの自動保存**(`MemoEditViewModel.persist()` が
`SaveMemoCommand` を組み立てて `SaveMemoUseCase` を呼ぶ)。画像もこのフローに統合する:

1. 添付 = ファイルを即コピー(`AttachMemoImageUseCase`)→ UI state の `images` に追加 → 既存デバウンスで `SaveMemoCommand.images` ごと保存
2. **画像は「コンテンツ」として扱う**。次の2箇所を必ずセットで変更する(片方だけだと「画像のみのメモが破棄される」か「保存時に例外」になる):
   - `SaveMemoUseCase` の require を「title / body / images のいずれか非空」に緩和
   - `MemoEditViewModel.isContentBlank()` に `images.isEmpty()` を追加
3. 1枚削除は、DB に保存済みの画像と未保存画像で後始末を分ける:
   - DB に保存済みの画像は UI state から取り除くだけ。ファイル削除は次回保存時に Repository が差分検出して行う(下記決定4)
   - 添付直後でまだ DB に保存されていない画像は、Repository の差分検出では拾えないため、ViewModel が `DeleteMemoImagesUseCase` で即座にファイル削除する

### 決定4: 孤児ファイルの削除は RoomMemoRepository に一元化する

DB 行は FK CASCADE で消えるがファイルは消えないため、「行を消す場所=ファイルを消す場所」を Repository 1クラスに閉じ込める。
パターンは全経路共通で **「トランザクション内で削除対象 fileName を収集 → コミット成功後にファイル削除」**
(コミット前にファイルを消すと rollback 時に参照切れが発生するため禁止):

| 経路 | 削除対象 |
|---|---|
| `saveMemo` | 保存前後の fileName 差分(UI で外された画像) |
| `deleteMemoPermanently` / `discardMemo` | そのメモの全画像ファイル |
| `deleteTrashedMemosDeletedAtOrBefore`(ゴミ箱自動削除) | 期限切れメモ群の全画像ファイル |
| `saveAllMemos` / `importAll`(インポート上書き) | 上書きで参照が消えた分 |

ゴミ箱への移動・復元はファイルに触らない(復元すれば画像も戻る)。
Repository で拾えないのは「DB に保存される前に UI state から消えた画像」。具体的には、ファイルコピー完了時には画面が既に終了していた添付レースと、添付直後・autosave 前に削除された未保存画像が該当する。
これらは ViewModel が `DeleteMemoImagesUseCase` で即座に後始末する(03 参照)。
プロセス強制終了のタイミング次第で稀に孤児ファイルが残り得るが、参照切れは UI のフォールバック表示で吸収される
(起動時の GC スイープは将来課題。PR 説明に明記すること)。

### 決定5: DB はマイグレーションなしでスキーマ直接変更

未リリースのため:

- `LiteMemoDatabase` の `version = 1` は据え置き、`entities` に `MemoImageEntity` を追加するだけ
- `LiteMemoMigrations.ALL` は `emptyArray()` のまま。Migration クラスは作らない
- `app/schemas/com.appvoyager.litememo.data.local.LiteMemoDatabase/1.json` はビルドで再生成される。
  **再生成後の 1.json を必ずコミットすること**(最典型の失敗ポイント)
- identityHash が変わるため、**旧スキーマの DB を持つ開発端末ではアプリをアンインストール(またはデータ消去)してから入れ直す**
- 既存の `LiteMemoMigrationInstrumentedTest` は「再生成後の v1 スキーマを生成して検証する」だけなので無変更で通る

### 決定6: サムネイルは Coil 3 で保存ファイルを直接読む

- `io.coil-kt.coil3:coil-compose` を追加(ローカルファイルのみなのでネットワーク用アーティファクトは不要)
- ファイル名は UUID ベースで内容不変(削除→再添付は必ず別 ID)のため、Coil のメモリキャッシュが陳腐化する経路がない
- メモカード右端に 56dp・角丸・`ContentScale.Crop` の `AsyncImage`。読み込みエラー時は同スロットに画像アイコン
- `MemoCard` はホームとカレンダーで共用なので、カレンダー画面のサムネ対応は自動で付いてくる

## 4. データフロー全体図

```text
[添付]
MemoEditRoute: PickMultipleVisualMedia → List<Uri>
  ※ 1回の選択数は Photo Picker / fallback 先の仕様に従う。アプリ側は保存枚数上限を追加しない
  → MemoEditViewModel.attachImages(uris.map { it.toString() })
  → AttachMemoImageUseCase(ImageSourceReference(uri))
  → MemoImageStore.saveImage: filesDir/memo_images/<uuid>.<ext> へコピー → MemoImage
  → uiState.images += MemoImageUiModel(id, fileName, filePath)(+ SavedStateHandle 下書き保存)
  → 1000ms デバウンス → persist() → SaveMemoCommand(images = …) → SaveMemoUseCase
  → RoomMemoRepository.saveMemo:
       tx { 旧fileName取得 → memos upsert → memo_images delete→insert }
       tx成功後: (旧 − 新) の fileName を MemoImageStore.deleteImages

[一覧表示]
Room → Memo(images) → HomeViewModel / CalendarViewModel
  → MemoUiModel.fromDomain(memos, tags, resolveImagePath)  // 先頭画像 → thumbnailPath
  → MemoCard: AsyncImage(File(thumbnailPath)) / エラー時はアイコン

[削除系]
添付直後・autosave 前に削除された未保存画像:
  ViewModel: UI state から削除 → DeleteMemoImagesUseCase で即時 deleteImages
完全削除・破棄・ゴミ箱自動削除・インポート上書き:
  Repository: tx { fileName収集 → 行削除 } → tx成功後 deleteImages
ゴミ箱移動・復元: ファイルに触らない

[export/import]
JSON フォーマット v1 のまま完全無変更。画像は対象外。
```

## 5. ブランチ構成と実施順

| # | ブランチ | base | 内容 | 単体で green な理由 |
|---|---|---|---|---|
| 1 | `feature/memo-image-domain` | `develop` | モデル / VO / `MemoImageStore` / UseCase。**動作は不変** | `Memo.images` にデフォルト値、かつ新インターフェースをどの ViewModel にも注入しない |
| 2 | `feature/memo-image-data` | ブランチ1 | Room エンティティ / DAO / schema 1.json 再生成 / mapper / Repository の差分ファイル削除 / DI バインディング | UI から画像を作る手段がまだ無いので挙動不変 |
| 3 | `feature/memo-image-ui` | ブランチ2 | Coil / Photo Picker / 編集・一覧 UI / ViewModel 配線 / strings | 全機能が結線され E2E で動作 |

**Hilt の段階的配線ルール(重要)**: `MemoImageStore` / `MemoImageIdProvider` の Hilt バインディングはブランチ2で追加する。
ブランチ1の時点でこれらを ViewModel に注入するとバインディング未定義でコンパイルが落ちるため、
**ブランチ1では `ui/` 配下を一切変更しない**。`SaveMemoUseCase` のコンストラクタは3ブランチを通して不変。

## 6. 横断的な注意点(gotchas)

1. **schema 1.json の再生成コミット忘れ**が最典型の失敗。ブランチ2の Done 条件に含めてある
2. **Kover allowlist(`app/build.gradle.kts` の `kover { classes(...) }`)は変更しない**。
   `MemoImageFileDataSource` は対象外の `data.image` パッケージへ、テスト可能なオーケストレーション層
   `FileSystemMemoImageStore` は対象内の `data.repository` へ、という配置を崩さない
3. strings は `values/strings.xml`(日本語・デフォルト)と `values-en/strings.xml` の**両方**に、同じセクション・同じ並びで追加
4. 「画像=コンテンツ」の2箇所セット変更(決定3-2)を片方だけにしない
5. ファイル削除はトランザクションコミット後(決定4)。コミット前に消さない
6. インポート上書き経路のファイル差分削除を忘れると孤児ファイルが溜まる。さらに、画像が export/import 対象外であることと、同一ID上書きで添付画像が失われることを import 確認文言にも明記する
7. Coil 3 の import は `coil3.compose.AsyncImage`。モデルには文字列パスでなく `java.io.File` を渡す(スキーム解釈のブレ回避)
8. `MemoWithTagRefs` → `MemoWithRefs` へのリネーム(ブランチ2)はコンパイラが参照漏れを検出してくれる。同ブランチ内で完結させる
9. 開発端末は identityHash 変更でクラッシュするためアンインストール→再インストール(決定5)
10. 添付直後・autosave 前に削除された未保存画像は DB 差分では検出できないため、ViewModel 側で即時ファイル削除する
