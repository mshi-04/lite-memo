# Unit Test

この文書は Lite Memo の Unit Test 方針をまとめます。

## 基本方針

- Unit Test は JUnit 5 を使う
- テスト関数名は英語にする
- 1つのテスト関数では、原則として1つの振る舞いを1つの主要な assert で検証する
- 複数条件を検証したい場合は、テスト関数を分ける
- Domain の値オブジェクト、UseCase、Repository interface 境界を優先してテストする

## テスト対象の優先度

JVM 上の Unit Test で優先してテストするもの:

- 値オブジェクトの生成条件と不正値
- UseCase の正常系と失敗系
- Repository interface を境界にした domain の振る舞い
- mapper の変換ルール

後回しにしてよいもの:

- Compose UI の細かい見た目
- まだ仕様が固まっていない画面構成

## 命名

- テストクラス名は対象クラス名に `Test` を付ける
- テスト関数名は英語で、検証する振る舞いが分かる名前にする
- 日本語の関数名やバッククォート関数名は使わない

例:

```kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MemoTitleTest {
    @Test
    fun constructorReturnsTrimmedValueWhenInputHasSurroundingWhitespace() {
        // Arrange
        val input = "  shopping  "

        // Act
        val title = MemoTitle(input)

        // Assert
        assertEquals("shopping", title.value)
    }
}
```

## Assert

- 1テスト関数につき、主要な assert は1つにする
- setup、実行、検証の意図が分かるように書く
- 複数の assert が必要に見える場合は、別の振る舞いが混ざっていないか確認する

## AAA Comments

- テストは Arrange / Act / Assert の流れで書く
- 基本は `// Arrange`、`// Act`、`// Assert` コメントを入れる
- 各ブロックの責務を混ぜない
- Arrange が不要な場合（準備するものがない）は `// Arrange` を省略してよい
- `// Arrange` / `// Act` / `// Assert` のラベルは置き換えず、常に残す
- 検証観点（後述）は、`// Act` の直下に `// 観点: 意図` の形で1行添える
- Turbine の `.test {}` のように操作と検証が交錯する場合は、ラベルを `// Act & Assert` にする

## テスト観点（命名とコメント）

テストが「何を検証しているか」を、関数名の接頭辞とコメントで明示します。
観点を明示することで、テストの意図と網羅状況が読み取りやすくなります。

### 観点の種類

| 観点 | 意味 |
|------|------|
| `Normal` | 正常系。期待どおりの入力で期待どおりの結果になる |
| `Boundary` | 境界・特殊入力（空リスト、重複、空白のみ、no-op など） |
| `Error` | 失敗系。例外の送出やエラーイベントの発火 |
| `Interaction` | 依存（Repository / Provider など）の呼び出し有無・回数・順序の検証（MockK の `verify` 系） |
| `Flow` | `StateFlow` / `Channel` event の発火を検証（Turbine の `.test {}` を使う） |
| `Coroutine` | 仮想時間・debounce・キャンセルなど coroutine の挙動 |
| `StateTransition` | 操作前後の UI state の遷移 |

### 適用ルール

- 関数名は観点を接頭辞に付ける（camelCase）。例: `normalUiStateLoadsExistingMemo`、`boundaryEmptyTagIdsSkipsTagValidation`、`flowSaveEmitsOperationErrorWhenMemoSaveFails`
- AAA ラベルは残したまま、`// Act`（または `// Act & Assert`）の直下に `// 観点: 意図` を1行置く。複数観点が絡む場合は `/` で連結する
- 観点は検証の主目的に合わせて選び、無理に増やさない

例:

```kotlin
@Test
fun boundaryEmptyTagIdsSkipsTagValidation() = runTest {
    // Arrange
    val tagRepository = mockk<TagRepository>()
    // ...

    // Act
    // Boundary/Interaction: empty tag ids do not touch TagRepository
    useCase(SaveMemoCommand(title = MemoTitle("Title"), body = MemoBody("Body")))

    // Assert
    coVerify(exactly = 0) { tagRepository.getTagsByIds(any()) }
    confirmVerified(tagRepository)
}
```

Turbine で操作と検証が交錯する場合:

```kotlin
@Test
fun flowSaveEmitsOperationErrorWhenMemoSaveFails() = runTest(dispatcher) {
    // Arrange
    val viewModel = memoEditViewModel(memoRepository = SaveFailingMemoRepository())
    advanceUntilIdle()

    // Act & Assert
    // Flow/Error: save failure emits SaveFailed
    viewModel.operationErrorEvent.test {
        viewModel.updateTitle("Title")
        viewModel.save()
        advanceUntilIdle()
        assertEquals(MemoEditOperationErrorEvent.SaveFailed, awaitItem())
    }
}
```

## Coroutine Test

- suspend 関数や Flow を扱う Unit Test は `kotlinx.coroutines.test.runTest` を使う
- `runBlocking` は既存テストの保守を除き、新規・更新する Unit Test では使わない
- 仮想時間制御、coroutine leak 検出、テスト間の分離を `runTest` に任せる

## Instrumented Test

Android Framework に強く依存する処理は `androidTest`（instrumented test）で検証します。

- Room の DAO とマイグレーション
- DataStore や ContentResolver を使う Repository 実装
- 必要に応じて Compose UI Test

instrumented test でも、テスト関数名は英語、`runTest` の方針をそろえます。
