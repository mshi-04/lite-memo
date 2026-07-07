# ブランチ1: `feature/memo-image-domain` — Domain 層

先に [`00-overview.md`](00-overview.md) を読むこと。

- **base**: `develop` / **PR 先**: `develop`
- **ゴール**: 画像添付のドメイン語彙(モデル・value object・store インターフェース・UseCase)を追加する。
  **アプリの動作は一切変わらない**(`images` は常に空。UI からこれらを呼ぶ経路がまだ無い)。
- **絶対条件**: `ui/` と `data/` のプロダクションコードに差分を作らない。
  `MemoImageStore` / `MemoImageIdProvider` の Hilt バインディングはまだ存在しないため、
  ViewModel 等に注入するとコンパイルが落ちる(Dagger はグラフから到達可能な依存のみ検証するので、
  未使用のうちはバインディング無しでもビルドが通る)。
- コミットメッセージ例: 「メモ画像添付のドメイン基盤を追加する」

パス prefix は `app/src/main/kotlin/com/appvoyager/litememo/`、テストは `app/src/test/kotlin/com/appvoyager/litememo/`。

## 1. 新規ファイル

### `domain/model/value/MemoImageId.kt`

既存 `domain/model/value/MemoId.kt` と同型(private constructor + companion `invoke`、非空白 require + trim)。

```kotlin
@JvmInline
value class MemoImageId private constructor(val value: String) {
    companion object {
        operator fun invoke(rawValue: String): MemoImageId {
            val value = rawValue.trim()
            require(value.isNotBlank()) { "MemoImageId must not be blank." }
            return MemoImageId(value)
        }
    }
}
```

### `domain/model/value/MemoImageFileName.kt`

保存ファイル名(例 `550e8400-….jpg`)の VO。パストラバーサル防止の検証を持つ。

```kotlin
@JvmInline
value class MemoImageFileName private constructor(val value: String) {
    companion object {
        operator fun invoke(rawValue: String): MemoImageFileName {
            val value = rawValue.trim()
            require(value.isNotBlank()) { "MemoImageFileName must not be blank." }
            require(!value.contains('/') && !value.contains('\\')) {
                "MemoImageFileName must not contain path separators."
            }
            require(value != "." && value != "..") {
                "MemoImageFileName must not be a relative path reference."
            }
            return MemoImageFileName(value)
        }
    }
}
```

### `domain/model/value/ImageSourceReference.kt`

Photo Picker が返した URI 文字列を包む VO。既存 `domain/model/value/ExportFileReference.kt` と同じ
「非空白 + 絶対 URI(`java.net.URI`)」検証をそのまま踏襲する(実装はほぼコピーで良い)。

### `domain/model/MemoImage.kt`

```kotlin
data class MemoImage(
    val id: MemoImageId,
    val fileName: MemoImageFileName
)
```

並び順の概念はリスト順で表現する(`Memo.images` / `SaveMemoCommand.images` の順序が正)。
position の数値化は data 層の責務(`Memo.tagIds` と `MemoTagRefEntity.position` の関係と同じ)。

### `domain/repository/MemoImageStore.kt`

配置は `ExportFileRepository` と同じ `domain/repository`。KDoc で責務と no-op 仕様を明記すること。

```kotlin
interface MemoImageStore {

    /**
     * 端末上の画像(Photo Picker が返した URI)をアプリ管理のストレージへコピーし、
     * 確定した画像メタデータを返す。コピーに失敗した場合は例外を投げる。
     */
    suspend fun saveImage(source: ImageSourceReference): MemoImage

    /** 指定した保存済み画像ファイルを削除する。存在しないファイルは無視する(no-op)。 */
    suspend fun deleteImages(fileNames: List<MemoImageFileName>)

    /** 表示用にファイルの絶対パス文字列を返す。ファイルの存在確認はしない。 */
    fun resolveImagePath(fileName: MemoImageFileName): String
}
```

### `domain/provider/MemoImageIdProvider.kt`

既存 `domain/provider/MemoIdProvider.kt` と同型。

```kotlin
interface MemoImageIdProvider {
    fun newMemoImageId(): MemoImageId
}
```

### UseCase 3つ(`domain/usecase/`)

いずれも既存 UseCase の規約どおり `class Xxx @Inject constructor(...)` + `operator fun invoke`。
3つとも `MemoImageStore` のみを注入する薄い委譲(検証やオーケストレーションは呼び出し側と store 実装の責務)。

| ファイル | シグネチャ |
|---|---|
| `AttachMemoImageUseCase.kt` | `suspend operator fun invoke(source: ImageSourceReference): MemoImage = memoImageStore.saveImage(source)` |
| `DeleteMemoImagesUseCase.kt` | `suspend operator fun invoke(fileNames: List<MemoImageFileName>) = memoImageStore.deleteImages(fileNames)` |
| `ResolveMemoImagePathUseCase.kt` | `operator fun invoke(fileName: MemoImageFileName): String = memoImageStore.resolveImagePath(fileName)`(非 suspend) |

## 2. 変更ファイル

### `domain/model/Memo.kt`

- `tagIds` の直後にプロパティ追加: `val images: List<MemoImage> = emptyList(),`
- 既存 `init` ブロックに require を1つ追加:

```kotlin
require(images.distinctBy { it.id }.size == images.size) {
    "Memo images must not contain duplicated ids."
}
```

既存の呼び出しは全て名前付き引数なのでデフォルト値追加で壊れないが、
念のため位置引数で `Memo(` を構築している箇所が無いことを確認してから着手する
(`grep -rn "Memo(" app/src --include=*.kt` で目視)。

### `domain/model/SaveMemoCommand.kt`

`tagIds` の直後に `val images: List<MemoImage> = emptyList(),` を追加。

### `domain/usecase/SaveMemoUseCase.kt`

変更は2点のみ。**コンストラクタ(依存)は変更禁止**(3ブランチを通して不変にする取り決め)。

1. 冒頭の require(現在は「title または body が非空白」)を緩和:

```kotlin
// タグやお気に入りだけではメモを保存しない。タイトル・本文・画像のいずれかが必須。
require(
    command.title.value.isNotBlank() ||
        command.body.value.isNotBlank() ||
        command.images.isNotEmpty()
) {
    "Memo title, body, or images must not be empty."
}
```

2. `Memo(...)` の生成(現在 `isFavorite = command.isFavorite` まで)に `images = command.images,` を追加。
   画像 ID の重複は `Memo` の init が拒否するので、この UseCase では追加検証しない。

## 3. テスト

既存の共有フィクスチャ `app/src/test/kotlin/com/appvoyager/litememo/domain/DomainTestFixtures.kt` を拡張:

- `memoFixture(...)` に引数 `images: List<MemoImage> = emptyList()` を追加して `Memo` へ渡す
- 追加: `fun memoImageFixture(id: String = "image-1", fileName: String = "image-1.jpg") = MemoImage(MemoImageId(id), MemoImageFileName(fileName))`
- 追加: `class FakeMemoImageStore : MemoImageStore`
  - `savedSources: MutableList<ImageSourceReference>` / `deletedFileNames: MutableList<MemoImageFileName>` を記録
  - `saveImage` は連番で `MemoImage(MemoImageId("image-N"), MemoImageFileName("image-N.jpg"))` を返す
  - `resolveImagePath` は `"/images/${fileName.value}"` を返す
  - 失敗を再現できるよう `var saveError: Throwable? = null` を持ち、非 null なら `saveImage` で throw

新規/追加テスト(すべて JVM、観点 prefix + AAA + 1 primary assert):

| テストクラス | ケース例 |
|---|---|
| `domain/model/value/MemoImageIdTest.kt`(新規) | `normalInvokeTrimsAndKeepsValue` / `errorInvokeThrowsWhenBlank` |
| `domain/model/value/MemoImageFileNameTest.kt`(新規) | `normalInvokeKeepsPlainFileName` / `errorInvokeThrowsWhenContainsSlash` / `errorInvokeThrowsWhenContainsBackslash` / `errorInvokeThrowsWhenDotDot` / `errorInvokeThrowsWhenBlank` |
| `domain/model/value/ImageSourceReferenceTest.kt`(新規) | 既存 `ExportFileReferenceTest` を踏襲(絶対URI OK / 相対・空白 NG) |
| `domain/model/MemoTest.kt`(追加) | `errorInitThrowsWhenImagesContainDuplicatedIds` |
| `domain/usecase/SaveMemoUseCaseTest.kt`(追加) | `normalInvokeSavesMemoWithCommandImages` / `normalInvokeSavesImageOnlyMemoWhenTitleAndBodyAreBlank` / `errorInvokeThrowsWhenImagesContainDuplicatedIds` |
| `domain/usecase/AttachMemoImageUseCaseTest.kt`(新規) | `normalInvokeReturnsSavedImageFromStore` / `interactionInvokeDelegatesSourceToStore` / `errorInvokePropagatesStoreFailure` |
| `domain/usecase/DeleteMemoImagesUseCaseTest.kt`(新規) | `interactionInvokeDelegatesFileNamesToStore` |
| `domain/usecase/ResolveMemoImagePathUseCaseTest.kt`(新規) | `normalInvokeReturnsResolvedPath` |

## 4. 検証コマンドと Done 条件

```sh
./gradlew app:ktlintCheck app:detekt :app:lintProdDebug :app:testProdDebugUnitTest
./gradlew :app:connectedDevDebugAndroidTest   # 既存テストが無変更で通ること
```

Done 条件:

- 上記すべて green
- `git diff --stat develop` に `ui/` と `data/` のプロダクションコード差分が無い(テストフィクスチャの `domain/` 配下は可)
- Kover allowlist(`app/build.gradle.kts`)は無変更(新規クラスはすべて既存対象パッケージ `domain.model.*` / `domain.model.value.*` / `domain.usecase.*` 内。`domain/repository`・`domain/provider` のインターフェースはもともと対象外)
