# Unit Test

この文書は Lite Memo の Unit Test 方針をまとめます。

## 基本方針

- Unit Test は JUnit 5 を使う
- テスト関数名は英語にする
- 1つのテスト関数では、原則として1つの振る舞いを1つの主要な assert で検証する
- 複数条件を検証したい場合は、テスト関数を分ける
- Domain の値オブジェクト、UseCase、Repository interface 境界を優先してテストする

## テスト対象の優先度

優先してテストするもの:

- 値オブジェクトの生成条件と不正値
- UseCase の正常系と失敗系
- Repository interface を境界にした domain の振る舞い
- mapper の変換ルール

後回しにしてよいもの:

- Compose UI の細かい見た目
- Android Framework に強く依存する処理
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
    fun createReturnsTitleWhenValueIsValid() {
        val title = MemoTitle.create("shopping")

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
- 流れが読み取りにくい場合は、`// Arrange`、`// Act`、`// Assert` コメントを使う
- テストが短く明らかな場合は、AAA コメントを省略してよい
- AAA コメントを入れる場合は、各ブロックの責務を混ぜない
