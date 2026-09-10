# GHOST HACKER: FAMILY

![test](https://github.com/com-junkawasaki/ghosthacker-family/actions/workflows/test.yml/badge.svg)

Ghost Hacker ゲームポートフォリオ第5弾。設計は
[ADR-2607023200](../../../90-docs/adr/2607023200-ghosthacker-game-portfolio-flow.md)
（superproject `com-junkawasaki/root`、addendum 2）を参照。

[Ghost Hacker](https://github.com/com-junkawasaki/ghosthacker)（既存カノン: Ren/Nei、
「情報は物理だ」、情報場、Ghost Battle / Daemon Battle）を土台に、FreeTEMPOの
『Life』（2010）収録曲 "Family" に由来する、10ジャンル展開の第5弾。

## コンセプト

- **ジャンル**: シミュレーション（経営シム）
- **主人公**: Ren & Nei共同（co-op）
- **コアループ**: 事務所併設リスニングバーを、固定の営業日数
  （`opening-week`、5日）にわたって切り盛りする。毎日、限られた
  attention pointの総量（`total-attention`=10）を`:drinks`/`:music`/
  `:staff-care`の3カテゴリに配分し、その日の（見せない）`:need`と
  照らし合わせて`:thriving`/`:steady`/`:struggling`の3段階で判定される
  — TUNINGのdial-vs-target整合判定を、単一値ではなく3カテゴリの配分
  ベクトルに一般化した形。判定tierは`:funds`（店が保つか）と
  `:family-bond`（見つけた家族=Familyとしての結びつきが本当に深まって
  いるか）という2つの累計に反映され、後者がこのタイトルのテーマそのもの
  — 経営シムの数字が found-family のメタファーではなく、found-family の
  進捗表になっている。全日程消化後、最終`:funds`・`:family-bond`・
  `bond-level`（`:estranged`〜`:found-family`）・総合`grade`
  （`:funds`が赤字だと`:family-bond`が高くてもトップグレードにはならない）
  のサマリで締める。

  他の実装済みタイトルと同じく、経済シミュレーションとしては意図的に
  単純（価格・在庫・複数日にまたがるサプライチェーン等は無い）— 1日1
  配分・1判定・1回のstate遷移(`resolve-day`)という、ECHOESの`choose`や
  TUNINGの`lock-in`と同型の、このコア唯一の日次遷移関数に絞ってある。

## 実装範囲

`src/ghosthacker_family/core.kotoba` — pure、host-free。判定/state核:

- `shift-allocation` — allocationの2カテゴリ間でattention pointを移動する
  （移動元残量・負値の両方をclampし、合計`total-attention`という不変
  条件を保つ）
- `mismatch`/`judge-mismatch`/`judge-day` — allocationとneedのカテゴリ別
  絶対差の合計から`:thriving`/`:steady`/`:struggling`を判定
- `resolve-day`/`current-day`/`complete?` — 現在dayを判定して次へ進める、
  全日程消化判定（このcore唯一の日次state遷移関数）
- `bond-level`/`grade`/`summary` — リザルト画面向けのサマリ

`src/ghosthacker_family/days.kotoba` — サンプルの完結した5日程
（`opening-week`）。

**プレイ可能な最小プロトタイプ**として `src/ghosthacker_family/terminal.kotoba`
がある。TUNINGと同様、実時間の判定が無いため`future`/agentスレッドプールを
一切使わない素朴なshift&lockのREPLループ。目標配分(`:need`)は直接表示せず、
`reception`（近さのみを示す0〜100%、方向は教えない）だけを手がかりに、
店の空気を読むような手触りにしている。

**ブラウザで遊べるホストアダプタ**が `src/ghosthacker_family/web.kotoba`
（reagent、ADR-2607100900 follow-up (b)）: FAMILYはリアルタイムの判定が
無いため、ECHOES/TUNINGと同じ低複雑度側の構成（Web Audio不要、ボタン
駆動のshift/lock UI）で足りる。

## 開発

```bash
clojure -M:test
```

Lint（clj-kondo、Clojars経由でHomebrew等の別インストール不要）:

```bash
clojure -M:lint
```

`main`へのpush/PRで `.github/workflows/test.yml` が自動でテスト+lintを実行する。

`src/ghosthacker_family/bounded.kotoba`は固定5日の`opening-week`に対し、
各日`drinks/music/staff-care`の非負整数3値・合計10を受け取るcapability-free
Kotobaプロファイル。全週と途中終了を扱い、不完全な日、負値、合計違反、
6日以上を拒否する。任意day set、allocation編集UI、host状態はCLJC oracleに残す。

ターミナルで遊んでみる:

```bash
clojure -M -m ghosthacker-family.terminal
```

ブラウザで遊んでみる（`npm install`は初回のみ）:

```bash
npm install
npx shadow-cljs watch app   # http://localhost:8299 で自動リロード開発
npx shadow-cljs release app # public/ に静的バンドルをビルド(デプロイ可能)
```

変更履歴は [CHANGELOG.md](CHANGELOG.md)。

## ライセンス

MIT License — [LICENSE](LICENSE) 参照。
