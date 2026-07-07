# ブランチ2: `feature/memo-image-data` — Data 層

先に [`00-overview.md`](00-overview.md) を読むこと。ブランチ1(`feature/memo-image-domain`)の成果物を前提とする。

- **base**: `feature/memo-image-domain` / **PR 先**: `develop`(ブランチ1マージ後に rebase して PR)
- **ゴール**: `memo_images` テーブルの追加(マイグレーションなし・version 1 据え置き)、画像ファイルストア実装、
  Repository での永続化と孤児ファイル削除、Hilt バインディング。
  UI からはまだ画像を作れないため、**アプリの見た目・挙動は不変**。
- コミットメッセージ例: 「メモ画像のRoom永続化とファイルストアを追加する」

パス prefix は `app/src/main/kotlin/com/appvoyager/litememo/`。

## 1. Room スキーマ変更(マイグレーションなし)

### 新規 `data/local/entity/MemoImageEntity.kt`

`MemoTagRefEntity` の先例(FK CASCADE + `[memoId, position]` ユニーク索引)を踏襲。
画像は複数メモで共有しないため `id` を単独 PK にする。

```kotlin
@Entity(
    tableName = "memo_images",
    foreignKeys = [
        ForeignKey(
            entity = MemoEntity::class,
            parentColumns = ["id"],
            childColumns = ["memoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["memoId", "position"], unique = true)
    ]
)
data class MemoImageEntity(
    @PrimaryKey val id: String,
    val memoId: String,
    val fileName: String,
    val position: Int
)
```

FK 子カラム `memoId` 単体の索引は不要(複合索引 `[memoId, position]` の先頭一致で満たされ、Room の lint 警告も出ない)。

### 変更 `data/local/LiteMemoDatabase.kt`

- `entities` 配列に `MemoImageEntity::class` を追加
- **`version = 1` のまま変更しない**

### `data/local/migration/LiteMemoMigrations.kt`

**変更しない**(`ALL = emptyArray()` のまま)。Migration クラスは作らない。

### スキーマ JSON の再生成

- ビルド(例: `./gradlew :app:kspDevDebugKotlin`)で
  `app/schemas/com.appvoyager.litememo.data.local.LiteMemoDatabase/1.json` が再生成される
  (identityHash が変わり、`memo_images` テーブル定義が加わる)
- **再生成後の 1.json を必ずコミットする**(最典型の失敗ポイント)
- 旧スキーマの DB を持つ開発端末では identityHash 不一致でクラッシュするため、アンインストール(またはデータ消去)して入れ直す。PR 説明にも明記する
- 既存 `app/src/androidTest/.../data/local/migration/LiteMemoMigrationInstrumentedTest.kt` は
  「schema JSON から v1 DB を生成して検証する」テストなので**無変更で通る**(memo_images は空テーブルとして作られるだけ)

## 2. Relation モデルと DAO

### `data/local/model/MemoWithTagRefs.kt` → `MemoWithRefs.kt` にリネーム

tag と image の両方を持つため名前を一般化する。

```kotlin
data class MemoWithRefs(
    @Embedded val memo: MemoEntity,
    @Relation(parentColumn = "id", entityColumn = "memoId")
    val tagRefs: List<MemoTagRefEntity>,
    @Relation(parentColumn = "id", entityColumn = "memoId")
    val imageRefs: List<MemoImageEntity>
)
```

参照箇所は機械的に追従する(コンパイラが漏れを検出する): `MemoDao` の戻り型6箇所、`MemoMapper`、
`MemoMapperTest`、`MemoDaoTest`、`RoomDaoInstrumentedTest` 等。
DAO のメソッド名も `observeActiveMemosWithTagRefs` → `observeActiveMemosWithRefs` のように `WithTagRefs` を `WithRefs` へ一括リネームする。

### `data/local/dao/MemoDao.kt` — 追加メソッド

```kotlin
@Insert
suspend fun insertImageRefs(imageRefs: List<MemoImageEntity>)

@Query("DELETE FROM memo_images WHERE memoId = :memoId")
suspend fun deleteImageRefsForMemo(memoId: String)

@Query("SELECT fileName FROM memo_images WHERE memoId = :memoId")
suspend fun getImageFileNamesForMemo(memoId: String): List<String>

@Query(
    """
    SELECT fileName FROM memo_images
    WHERE memoId IN (SELECT id FROM memos WHERE deletedAt IS NOT NULL AND deletedAt <= :cutoff)
    """
)
suspend fun getImageFileNamesForTrashedMemosDeletedAtOrBefore(cutoff: Long): List<String>
```

### `data/local/dao/MemoDao.kt` — トランザクション default メソッドの拡張

既存 `upsertMemoWithTags(memo, tagRefs)` を `upsertMemoWithRefs(memo, tagRefs, imageRefs)` に改名・拡張
(tag 側の delete→insert パターンを image にも複製):

```kotlin
@Transaction
suspend fun upsertMemoWithRefs(
    memo: MemoEntity,
    tagRefs: List<MemoTagRefEntity>,
    imageRefs: List<MemoImageEntity>
) {
    require(tagRefs.all { it.memoId == memo.id }) {
        "All tagRefs must reference memoId=${memo.id}."
    }
    require(imageRefs.all { it.memoId == memo.id }) {
        "All imageRefs must reference memoId=${memo.id}."
    }

    upsertMemo(memo)
    deleteTagRefsForMemo(memo.id)
    if (tagRefs.isNotEmpty()) {
        insertTagRefs(tagRefs)
    }
    deleteImageRefsForMemo(memo.id)
    if (imageRefs.isNotEmpty()) {
        insertImageRefs(imageRefs)
    }
}
```

同様に `upsertAllMemosWithTags(memos, tagRefsByMemoId)` を
`upsertAllMemosWithRefs(memos, tagRefsByMemoId, imageRefsByMemoId)` に改名・拡張する。

## 3. Mapper(`data/mapper/MemoMapper.kt`)

既存規約どおり自由関数の拡張。VO は出口で `.value`、入口で factory `invoke`。

- 追加 `fun Memo.toImageRefs()` — `images.mapIndexed { index, image -> MemoImageEntity(id = image.id.value, memoId = id.value, fileName = image.fileName.value, position = index) }`
- 変更 `MemoEntity.toDomain(tagRefs)` → `toDomain(tagRefs, imageRefs)` に引数追加:
  - `require(imageRefs.all { it.memoId == id })`(tagRefs と同じメッセージ形式)
  - `images = imageRefs.sortedBy { it.position }.map { MemoImage(MemoImageId(it.id), MemoImageFileName(it.fileName)) }`
- 変更 `MemoWithRefs.toDomain() = memo.toDomain(tagRefs, imageRefs)`

## 4. 画像ファイルストア実装

### 新規 `data/image/MemoImageFileDataSource.kt`

Context / ContentResolver / File I/O の生実装。既存 `data/export/ExportFileWriter.kt` の作りを踏襲
(`@param:ApplicationContext private val context: Context`, `@param:IoDispatcher private val ioDispatcher: CoroutineDispatcher`、
処理は `withContext(ioDispatcher)` で包む)。
**Kover 対象外の `data.image` パッケージに置くこと(00 の gotcha 2)。**

```kotlin
class MemoImageFileDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    /** source URI の MIME type から拡張子を解決する。解決できなければ null。 */
    suspend fun detectExtension(sourceUri: String): String?
        // contentResolver.getType(Uri.parse(sourceUri))
        //   ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }

    /** source URI の内容を memo_images/<fileName> へコピーする。失敗時は書きかけファイルを消して例外を投げる。 */
    suspend fun copyImage(sourceUri: String, fileName: String)
        // imagesDir().mkdirs()
        // openInputStream(uri)?.use { input -> File(imagesDir(), fileName).outputStream().use { input.copyTo(it) } }
        //   ?: throw IOException("Failed to open input stream for URI: $sourceUri")
        // catch (IOException) { target.delete(); throw }

    /** memo_images/<fileName> を削除する。存在しなければ no-op。 */
    suspend fun deleteImage(fileName: String)

    /** 表示用の絶対パスを返す(存在確認なし)。 */
    fun imageFilePath(fileName: String): String

    private fun imagesDir(): File = File(context.filesDir, IMAGES_DIR)

    companion object {
        const val IMAGES_DIR = "memo_images"
    }
}
```

### 新規 `data/repository/FileSystemMemoImageStore.kt`

ID 採番・ファイル名組み立て・委譲のオーケストレーション。**Kover 対象の `data.repository` に置く**(MockK で JVM test する)。

```kotlin
class FileSystemMemoImageStore @Inject constructor(
    private val dataSource: MemoImageFileDataSource,
    private val memoImageIdProvider: MemoImageIdProvider
) : MemoImageStore {

    override suspend fun saveImage(source: ImageSourceReference): MemoImage {
        val id = memoImageIdProvider.newMemoImageId()
        val extension = dataSource.detectExtension(source.value) ?: FALLBACK_EXTENSION
        val fileName = MemoImageFileName("${id.value}.$extension")
        dataSource.copyImage(source.value, fileName.value)
        return MemoImage(id = id, fileName = fileName)
    }

    override suspend fun deleteImages(fileNames: List<MemoImageFileName>) {
        fileNames.forEach { dataSource.deleteImage(it.value) }
    }

    override fun resolveImagePath(fileName: MemoImageFileName): String =
        dataSource.imageFilePath(fileName.value)

    private companion object {
        const val FALLBACK_EXTENSION = "img"
    }
}
```

### 新規 `data/provider/UuidMemoImageIdProvider.kt`

既存 `UuidMemoIdProvider` と同型: `override fun newMemoImageId(): MemoImageId = MemoImageId(UUID.randomUUID().toString())`

## 5. Repository(`data/repository/RoomMemoRepository.kt`)

コンストラクタに4引数目 `private val memoImageStore: MemoImageStore` を追加。
方針は 00 の決定4: **「tx 内で削除対象 fileName を収集 → コミット成功後にファイル削除」**。

共通ヘルパを private で追加:

```kotlin
private suspend fun deleteImageFiles(fileNames: Collection<String>) {
    if (fileNames.isEmpty()) return
    memoImageStore.deleteImages(fileNames.map { MemoImageFileName(it) })
}
```

各メソッドの変更(現在 `saveMemo` は DAO の `@Transaction` メソッド呼び出しのみで `database.withTransaction` を使っていない。
差分収集と upsert を原子化するため明示的に `withTransaction` で包み直す):

```kotlin
override suspend fun saveMemo(memo: Memo) {
    val removedFileNames = database.withTransaction {
        val before = memoDao.getImageFileNamesForMemo(memo.id.value)
        memoDao.upsertMemoWithRefs(memo.toEntity(), memo.toTagRefs(), memo.toImageRefs())
        before - memo.images.map { it.fileName.value }.toSet()
    }
    deleteImageFiles(removedFileNames)
}
```

- `deleteMemoPermanently(id)` — `withTransaction { fileNames = getImageFileNamesForMemo; affected = dao.deleteMemoPermanently; check(affected > 0) { … }; fileNames }` → tx 後 `deleteImageFiles`。
  `check` 失敗は例外で rollback するのでファイルも消えない(正しい挙動)
- `discardMemo(id)` — 同パターン(check なし。既存どおり0件でも黙認)
- `deleteTrashedMemosDeletedAtOrBefore(cutoff)` — `withTransaction { fileNames = dao.getImageFileNamesForTrashedMemosDeletedAtOrBefore(cutoff.value); dao.deleteTrashedMemosDeletedAtOrBefore(cutoff.value); fileNames }` → tx 後削除
- `saveAllMemos(memos)` — `withTransaction` で包み、メモごとに `getImageFileNamesForMemo` で before を収集してから
  `upsertAllMemosWithRefs` を呼び、(before 合算 − 新 images 合算)を tx 後に削除
- `importAll` / `executeImport` — 既に `withTransaction` 内。`executeImport` の中で同様に before を収集し、
  削除対象を戻り値で返して `importAll` が tx 後に `deleteImageFiles` する形に変更
  (インポートされる `Memo` は export に画像が無いため `images = emptyList()`。上書きされたメモの画像ファイルはここで消える)
- `moveMemoToTrash` / `restoreMemoFromTrash` — **変更しない**(ファイルに触らない)
- 読み取り系は `MemoWithRefs` リネームへの追従のみ

## 6. DI(`data/di/`)

- `RepositoryModule.kt` に追加:

```kotlin
@Binds
@Singleton
abstract fun bindMemoImageStore(store: FileSystemMemoImageStore): MemoImageStore
```

- `ProviderModule.kt` に追加:

```kotlin
@Provides
@Singleton
fun provideMemoImageIdProvider(): MemoImageIdProvider = UuidMemoImageIdProvider()
```

- `DaoModule.kt` / `DatabaseModule.kt` は変更不要(新 DAO インターフェースは作らず `MemoDao` を拡張しているため)

## 7. テスト

### JVM(`app/src/test/kotlin/com/appvoyager/litememo/`)

| テストクラス | 追加ケース例 |
|---|---|
| `data/mapper/MemoMapperTest.kt` | `normalToImageRefsAssignsPositionsInListOrder` / `normalToDomainSortsImagesByPosition` / `errorToDomainThrowsWhenImageRefsReferenceAnotherMemo` |
| `data/local/dao/MemoDaoTest.kt` | 既存の recording 実装を拡張し、`upsertMemoWithRefs` の「upsert → tagRefs delete/insert → imageRefs delete/insert」の呼び出し順、空リスト時の insert スキップ、memoId 不一致 require を検証 |
| `data/repository/RoomMemoRepositoryTest.kt` | `interactionSaveMemoDeletesRemovedImageFilesAfterUpsert` / `interactionSaveMemoDoesNotDeleteFilesWhenImagesUnchanged` / `interactionDeleteMemoPermanentlyDeletesImageFiles` / `errorDeleteMemoPermanentlyDoesNotDeleteFilesWhenMemoMissing` / `interactionPurgeDeletesImageFilesOfExpiredTrashedMemos` / `interactionImportAllDeletesImageFilesOfOverwrittenMemos`(MemoDao / MemoImageStore は MockK。`database.withTransaction` は既存テストのモック手法を踏襲) |
| `data/repository/FileSystemMemoImageStoreTest.kt`(新規) | `normalSaveImageBuildsFileNameFromIdAndDetectedExtension` / `boundarySaveImageFallsBackToImgExtensionWhenMimeUnknown` / `errorSaveImagePropagatesCopyFailure` / `interactionDeleteImagesDelegatesEachFileName`(dataSource / provider は MockK) |

### androidTest(`app/src/androidTest/kotlin/com/appvoyager/litememo/`、JUnit 4)

| テストクラス | 内容 |
|---|---|
| `data/local/dao/RoomDaoInstrumentedTest.kt`(追加) | imageRefs 込みの `upsertMemoWithRefs` → observe で round-trip(position 順で返ること、上書きで差し替わること) |
| `data/image/MemoImageFileDataSourceInstrumentedTest.kt`(新規) | 一時ファイル + `Uri.fromFile` を source にして `copyImage` の実コピー、`deleteImage` の削除と no-op、`imageFilePath` の解決を検証。`detectExtension` は file:// URI では null(フォールバック経路)になることを確認。content:// での MIME 解決まで検証する場合は既存の `NonTruncatingExportTestProvider` の test provider 先例に倣う |
| `data/local/migration/LiteMemoMigrationInstrumentedTest.kt` | **変更しない**(再生成後の 1.json で通ることを確認するだけ) |

## 8. 検証コマンドと Done 条件

```sh
./gradlew app:ktlintCheck app:detekt :app:lintProdDebug :app:testProdDebugUnitTest
./gradlew :app:connectedDevDebugAndroidTest
```

Done 条件:

- 上記すべて green(migration テスト含む)
- **再生成された `app/schemas/.../1.json` がコミットに含まれている**(`git status` で確認)
- `LiteMemoDatabase.version == 1`、`LiteMemoMigrations.ALL` は空のまま、`DatabaseModule` 無変更
- Kover allowlist 無変更(`data.image` 配下は対象外、`FileSystemMemoImageStore` は `data.repository` 配下)
- `ui/` のプロダクションコード差分なし
