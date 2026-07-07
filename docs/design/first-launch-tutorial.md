# 設計書: 初回起動チュートリアル画面

## 背景と目的

初回起動時のユーザーは、メモゼロの状態でホーム画面の空状態メッセージだけを頼りに操作を始めることになる。
タグ・検索・カレンダーなど初見では気づきにくい機能の価値を伝えるため、初回起動時にのみ表示するチュートリアル画面を追加する。

「軽量メモアプリ」というコンセプトを損なわないよう、以下を必須条件とする。

- 3 ページ構成に収める
- どのページからでも 1 タップでスキップできる
- 一度完了(スキップ含む)したら二度と表示しない

## 前提(必読)

実装前に以下を確認すること。

- [`AGENTS.md`](../../AGENTS.md)
- [`docs/architecture.md`](../architecture.md)(依存方向 `ui -> domain <- data`)
- [`docs/implementation-guidelines.md`](../implementation-guidelines.md)
- [`docs/unit-test.md`](../unit-test.md)(AAA コメント、観点 prefix)
- Skill: `.agents/skills/ui-implementation/SKILL.md`、`domain-implementation`、`data-implementation`、`test-implementation`

## 全体方針

- 初回起動判定は「チュートリアル完了フラグ」を DataStore に保存して行う(Room を使うほどの構造化データではない)
- 表示のゲートは Navigation ではなく **`MainActivity` レベル**で行う。既存の `AppLockScreen` と同じレイヤーで、表示順は「アプリロック解除 → チュートリアル → 本体(`LiteMemoApp`)」とする
  - アプリロック有効なユーザーは既存ユーザーなので、実運用上この順で衝突しない。アップグレード直後(フラグ未保存)でもロックが先に出るため安全側に倒れる
- フラグは DataStore から非同期に読むため、**LOADING / VISIBLE / HIDDEN の 3 状態**で扱い、読み込み中に本体やチュートリアルが一瞬表示される「ちらつき」を防ぐ
- 既存ユーザーもアップデート後の初回起動で一度チュートリアルを見ることになるが、許容する(スキップ 1 タップで閉じられる)

## 変更ファイル一覧

### 新規

| ファイル | 内容 |
|---|---|
| `app/src/main/kotlin/com/appvoyager/litememo/domain/usecase/ObserveTutorialCompletedUseCase.kt` | 完了フラグの監視 |
| `app/src/main/kotlin/com/appvoyager/litememo/domain/usecase/CompleteTutorialUseCase.kt` | 完了フラグの保存 |
| `app/src/main/kotlin/com/appvoyager/litememo/ui/state/TutorialUiState.kt` | `TutorialStatus` enum |
| `app/src/main/kotlin/com/appvoyager/litememo/ui/screen/TutorialScreen.kt` | チュートリアル画面(stateless) |
| `app/src/test/kotlin/com/appvoyager/litememo/domain/usecase/ObserveTutorialCompletedUseCaseTest.kt` | UseCase test |
| `app/src/test/kotlin/com/appvoyager/litememo/domain/usecase/CompleteTutorialUseCaseTest.kt` | UseCase test |

### 変更

| ファイル | 内容 |
|---|---|
| `domain/repository/UserSettingsRepository.kt` | `observeTutorialCompleted()` / `completeTutorial()` を追加 |
| `data/repository/DataStoreUserSettingsRepository.kt` | 上記の実装と `TUTORIAL_COMPLETED_KEY` 追加 |
| `ui/viewmodel/MainViewModel.kt` | `tutorialStatus: StateFlow<TutorialStatus>` と `completeTutorial()` を追加 |
| `MainActivity.kt` | アプリロック解除後の表示分岐に `TutorialStatus` を追加 |
| `app/src/main/res/values/strings.xml` | チュートリアル文言(日本語) |
| `app/src/main/res/values-en/strings.xml` | チュートリアル文言(英語) |
| `app/src/test/kotlin/com/appvoyager/litememo/domain/repository/FakeUserSettingsRepository.kt` | 新メソッドの Fake 実装 |
| `app/src/test/kotlin/com/appvoyager/litememo/data/repository/DataStoreUserSettingsRepositoryTest.kt` | フラグの default / 保存テスト追加 |
| `app/src/test/kotlin/com/appvoyager/litememo/ui/viewmodel/MainViewModelTest.kt` | コンストラクタ変更への追随と tutorial 状態遷移テスト追加 |

Hilt binding は既存の `data/di/RepositoryModule.kt` の `UserSettingsRepository` バインドをそのまま使うため、**DI モジュールの変更は不要**。

## Domain 層

`UserSettingsRepository` に以下を追加する。完了フラグは一方向(true にしかならない)なので、`setTutorialCompleted(Boolean)` ではなく引数なしの `completeTutorial()` とし、不要な公開 API を作らない。

```kotlin
interface UserSettingsRepository {
    // 既存メソッド...
    fun observeTutorialCompleted(): Flow<Boolean>
    suspend fun completeTutorial()
}
```

UseCase は既存の `ObserveAppLockEnabledUseCase` / `SetAppLockEnabledUseCase` と同じ形式で 1 操作 1 UseCase。

```kotlin
class ObserveTutorialCompletedUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = userSettingsRepository.observeTutorialCompleted()
}

class CompleteTutorialUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) {
    suspend operator fun invoke() {
        userSettingsRepository.completeTutorial()
    }
}
```

## Data 層

`DataStoreUserSettingsRepository` に追加する。既存の `preferencesFlow`(`dataOrEmptyOnIoError()`)を使い、IO エラー時は未完了(false)として扱う。

```kotlin
override fun observeTutorialCompleted(): Flow<Boolean> = preferencesFlow.map { prefs ->
    prefs[TUTORIAL_COMPLETED_KEY] ?: false
}

override suspend fun completeTutorial() {
    dataStore.edit { prefs -> prefs[TUTORIAL_COMPLETED_KEY] = true }
}

// internal companion object に追加
val TUTORIAL_COMPLETED_KEY = booleanPreferencesKey("tutorial_completed")
```

## UI 層

### TutorialStatus(`ui/state/TutorialUiState.kt`)

```kotlin
enum class TutorialStatus {
    LOADING,  // フラグ読み込み中。何も出さない(ちらつき防止)
    VISIBLE,  // 未完了。チュートリアルを表示
    HIDDEN    // 完了済み。本体を表示
}
```

### MainViewModel

既存の `AppLockUiState` の扱いと同様に `MutableStateFlow` で保持する。

- コンストラクタに `ObserveTutorialCompletedUseCase` と `CompleteTutorialUseCase` を追加
- `tutorialStatus: StateFlow<TutorialStatus>`(初期値 `LOADING`)を公開
- `init` で `observeTutorialCompletedUseCase()` を collect し、次の規則で更新する
  - `completed == true` → `HIDDEN`
  - `completed == false` かつ現在 `LOADING` → `VISIBLE`
  - それ以外 → 現状維持(一度 `HIDDEN` にした後、保存失敗などで false が再通知されても同一起動中は再表示しない)
- `completeTutorial()`: 先に `_tutorialStatus.value = HIDDEN` にしてから `viewModelScope.launch` で `completeTutorialUseCase()` を呼ぶ(保存を待たずに閉じる)。例外は `CancellationException` を再 throw し、それ以外は握りつぶす(既存 `LiteMemoAppViewModel.restoreMemo` と同じ形式)。保存に失敗した場合は次回起動時に再表示されるだけなので許容する

### TutorialScreen(`ui/screen/TutorialScreen.kt`)

状態を持たない stateless Composable。ViewModel には直接依存せず、コールバックだけ受け取る。

```kotlin
@Composable
fun TutorialScreen(
    onCompleteTutorial: () -> Unit,
    modifier: Modifier = Modifier
)
```

- レイアウト(上から順に):
  1. 右上に「スキップ」`TextButton`(全ページで表示、タップで `onCompleteTutorial`)
  2. `HorizontalPager`(`androidx.compose.foundation.pager`、`weight(1f)`)。各ページは中央揃えの Column(アイコン 64dp / タイトル `headlineSmall` / 本文 `bodyMedium`、横 padding 32dp)
  3. ページインジケータ(8dp の丸ドット、現在ページは `primary`、他は `surfaceVariant`)
  4. 下部ボタン(`fillMaxWidth`、横 padding 32dp): 最終ページ以外は「次へ」で `animateScrollToPage(current + 1)`、最終ページは「はじめる」で `onCompleteTutorial`
- ルートは `Surface(color = MaterialTheme.colorScheme.background)`。`MainActivity` は edge-to-edge なので Column に `safeDrawingPadding()` を付ける
- 色は固定値を使わず Material 3 テーマから取る(ライト / ダーク両対応)
- `@Preview` を付けた private な Preview 関数を追加する

ページ内容(3 ページ、アイコンは `material-icons` 既存依存から):

| ページ | アイコン | タイトル | 本文の趣旨 |
|---|---|---|---|
| 1 | `Icons.Default.Edit` | LiteMemo へようこそ | すぐ書ける軽量メモアプリ。ホームの作成ボタンから追加 |
| 2 | `Icons.Default.Search` | タグと検索で整理 | タグ分類とキーワード検索 |
| 3 | `Icons.Default.DateRange` | カレンダーで振り返り | 日付ごとのメモ確認。さっそく始めよう |

ページ定義は `TutorialScreen.kt` 内の private data class(icon + `@get:StringRes` の titleResId / bodyResId)のリストとして持つ。

### MainActivity

`mainViewModel.tutorialStatus` を `collectAsStateWithLifecycle()` で収集し、既存分岐を次のように変更する。

```kotlin
LiteMemoTheme(darkTheme = darkTheme) {
    if (appLockUiState.canShowAppContent) {
        when (tutorialStatus) {
            // 読み込み中は背景だけ表示してちらつきを防ぐ
            TutorialStatus.LOADING -> Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {}

            TutorialStatus.VISIBLE -> TutorialScreen(
                onCompleteTutorial = { mainViewModel.completeTutorial() }
            )

            TutorialStatus.HIDDEN -> LiteMemoApp(/* 既存のまま */)
        }
    } else {
        AppLockScreen(/* 既存のまま */)
    }
}
```

## 文言(strings.xml)

`values/strings.xml`(日本語)と `values-en/strings.xml`(英語)の両方に、`<!-- Tutorial -->` セクションとして Common セクションの直後に追加する。長い英語文字列でもレイアウトが崩れないことを確認する。

| key | ja | en |
|---|---|---|
| `tutorial_skip` | スキップ | Skip |
| `tutorial_next` | 次へ | Next |
| `tutorial_start` | はじめる | Get started |
| `tutorial_page_welcome_title` | LiteMemo へようこそ | Welcome to LiteMemo |
| `tutorial_page_welcome_body` | 思いついたことをすぐに書き残せる、軽量メモアプリです。ホーム画面の作成ボタンからメモを追加できます。 | A lightweight memo app for quickly capturing your thoughts. Add memos with the create button on the home screen. |
| `tutorial_page_organize_title` | タグと検索で整理 | Organize with tags and search |
| `tutorial_page_organize_body` | メモにタグを付けて分類したり、キーワード検索で目的のメモをすぐに見つけられます。 | Categorize memos with tags and find the one you need instantly with keyword search. |
| `tutorial_page_calendar_title` | カレンダーで振り返り | Look back with the calendar |
| `tutorial_page_calendar_body` | カレンダーから日付ごとのメモを確認できます。準備ができたら、さっそく始めましょう。 | Review your memos by date on the calendar. When you are ready, let\'s get started. |

英語の `let\'s` のようにアポストロフィはエスケープすること。

## テスト

`docs/unit-test.md` の規約(JUnit 5 / `runTest` / AAA コメント / 観点 prefix / 1 テスト 1 assert)に従う。

1. **FakeUserSettingsRepository**(変更): `tutorialCompleted = MutableStateFlow(false)` を追加し、新 2 メソッドを実装
2. **ObserveTutorialCompletedUseCaseTest**(新規):
   - `normal`: デフォルトで false を返す
   - `normal`: `completeTutorial()` 後に true を返す
3. **CompleteTutorialUseCaseTest**(新規):
   - `normal`: invoke で完了フラグが永続化される
4. **DataStoreUserSettingsRepositoryTest**(変更): 既存の appLock テストと同形式で
   - デフォルトで false
   - `completeTutorial()` 後に true
5. **MainViewModelTest**(変更): ヘルパー `mainViewModel()` に新 UseCase を追加した上で
   - `stateTransition`: 未完了なら collect 後に `VISIBLE`
   - `stateTransition`: 完了済みなら collect 後に `HIDDEN`
   - `stateTransition`: `completeTutorial()` 呼び出しで即座に `HIDDEN`
   - `normal`: `completeTutorial()` で repository に完了が永続化される

Compose UI Test(androidTest)は必須としない(`docs/unit-test.md` の優先度に従い後回し可)。追加する場合は「スキップタップで `onCompleteTutorial` が呼ばれる」「最終ページのボタン文言が『はじめる』になる」の 2 点。

## 受け入れ条件

- [ ] 初回起動(フラグ未保存)でチュートリアルが表示される
- [ ] スキップまたは最終ページの「はじめる」でホーム画面に遷移し、フラグが保存される
- [ ] 2 回目以降の起動ではチュートリアルが表示されない(ちらつきもしない)
- [ ] アプリロック有効時はロック解除が先に表示される
- [ ] ライト / ダークテーマ両方で表示が崩れない
- [ ] 日本語 / 英語の両言語で文言が表示される
- [ ] `ktlint` / `detekt` / Android Lint / 既存 Unit Test がすべて通る

## 実装しないこと(スコープ外)

- チュートリアルの再表示導線(設定画面からの再表示)
- 画像・アニメーション素材の追加(アイコン + テキストのみで構成する)
- Room スキーマ変更、DB migration(不要)
