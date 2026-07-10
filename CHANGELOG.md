# Changelog

pure `.cljc` 経営シム核（`ghosthacker-family.core`）と、それを使う
プロトタイプ実装の変更履歴（ADR-2607023200、portfolio title #5）。

## Unreleased

- 初期実装: `core.cljc`（allocation-vs-need判定、`:thriving`/`:steady`/
  `:struggling`、`:funds`/`:family-bond`累計、`bond-level`/`grade`）、
  `days.cljc`（サンプル日程`opening-week`、5日）、`terminal.clj`
  （プレイ可能なshift&lock REPLプロトタイプ、`reception`のみでneedを
  教えない）、`web.cljs`（ブラウザhostアダプタ、reagent、ADR-2607100900
  follow-up (b)）。headless DOM上で実クリック操作による通し（5日
  shift→lock→result画面→もう一度で初期状態に復帰）を検証済み。
- テスト: `core_test.cljc`（13 assertions超、shift-allocationのclamp
  境界・judge-mismatch/judge-dayの3段階境界・resolve-day/complete?・
  bond-level/gradeの閾値・play/play-summary）+ `terminal_test.clj`
  （private var経由でreception-pct/tend-day!/play-loop!を直接検証、
  すべて整数値のためfloat epsilon比較は不要）。`clojure -M:lint`
  0エラー/0警告、`npx shadow-cljs compile app` 0警告を確認。
