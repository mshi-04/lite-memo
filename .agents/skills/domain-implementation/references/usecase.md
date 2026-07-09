# UseCase

1 操作 1 UseCase で、validation と business rule を UI / data へ漏らさない。

## 確認する対象

- `domain/usecase/` の既存 UseCase と命名（`Observe` / `Get` / `Save` / `Delete` / `Apply` など）
- 依存する Repository interface / provider / value object

## 実装時の注意

- 1 つの UseCase は 1 つの明確な操作を表す。
- UI から Repository 実装を直接呼ばせず、UseCase を境界に置く。
- Flow を返す観測系と、suspend の実行系を混ぜない。
- business rule を UI / data 側へ再実装しない。

## テスト判断

- 分岐・境界・失敗経路を JVM Unit Test で押さえる。
- Repository interface は fake / MockK で差し替える。

## 検証観点

- 単純な委譲だけの UseCase を不要に増やしていないか（結合を避ける必要があるときだけ挟む）。
- 例外・空・境界時の振る舞いが定義されているか。
