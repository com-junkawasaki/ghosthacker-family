(ns ghosthacker-family.days
  "GHOST HACKER: FAMILY -- sample day/need set (pure data, ADR-2607023200).

  \"opening-week\": 事務所併設リスニングバーの開店1週間、5営業日ぶんの
  `:need`(その日プレイヤーには見せない、Ren&Neiが後から『読み違えた/
  読めた』と振り返る想定の理想配分)。各dayのneedはtotal-attention(10)に
  正規化済み。プレイヤーには`terminal.clj`/`web.cljs`側で『reception』
  (近さのみを示す0〜100%、方向は教えない)としてしか提示しない -- ここに
  あるのはロジック用の生の数値のみ。"
  (:require [ghosthacker-family.core :as core]))

(def opening-week
  [{:label :soft-open
    :need {:drinks 5 :music 3 :staff-care 2}}
   {:label :regulars-return
    :need {:drinks 4 :music 4 :staff-care 2}}
   {:label :rainy-slump
    :need {:drinks 2 :music 3 :staff-care 5}}
   {:label :live-session-night
    :need {:drinks 5 :music 4 :staff-care 1}}
   {:label :anniversary
    :need {:drinks 3 :music 3 :staff-care 4}}])

(defn play-opening-week
  "opening-weekをallocationsで再生し、summaryを返す(core/play-summaryの
  薄いラッパー)。"
  [allocations]
  (core/play-summary opening-week allocations))
