(ns ghosthacker-family.terminal
  "GHOST HACKER: FAMILY -- minimal terminal host adapter (playable prototype).

  Like ghosthacker-tuning's terminal, FAMILY has no real-time pressure to
  track -- no `future`/agent thread pool, no wall-clock judging. Just a
  plain shift-and-lock REPL loop: each day Ren&Nei nudge attention points
  between two categories at a time (`dm` = 1pt drinks->music, etc, six
  two-letter commands total) and lock the day in (l) once the day's
  'reception' readout (proximity only, no direction -- read the room by
  feel) sounds right.

  Run: clojure -M -m ghosthacker-family.terminal"
  (:require [clojure.string :as str]
            [ghosthacker-family.core :as core]
            [ghosthacker-family.days :as days]))

(def ^:private step 1)

(def ^:private cat-codes
  "2文字コマンドの1文字目/2文字目に使うカテゴリ略号。"
  {"d" :drinks "m" :music "s" :staff-care})

(defn- parse-transfer
  "2文字コマンド(例: \"dm\")を[from-cat to-cat]に解決する。略号として
   無効、あるいはfrom=toなら nil(無効コマンド扱い)。"
  [cmd]
  (when (= 2 (count cmd))
    (let [from (get cat-codes (subs cmd 0 1))
          to (get cat-codes (subs cmd 1 2))]
      (when (and from to (not= from to))
        [from to]))))

(defn- reception-pct
  "allocationとneedのmismatchを、0(space読めてない)〜100(ぴったり)%の
   reception表示に変換する(値が大きいほど良い -- リスニングバーらしく、
   TUNINGのstatic(ノイズ量、低いほど良い)とは向きを逆にしてある)。"
  [allocation need]
  (let [m (core/mismatch allocation need)
        worst (* 2 core/total-attention)]
    (long (Math/round (* 100.0 (- 1.0 (/ (double (min m worst)) worst)))))))

(defn- print-day! [day allocation]
  (println (format "-- %s -- reception: %d%%  (drinks %d / music %d / staff-care %d)"
                    (name (:label day))
                    (reception-pct allocation (:need day))
                    (:drinks allocation) (:music allocation) (:staff-care allocation))))

(defn- read-command! []
  (print "[dm/ds/md/ms/sd/sm/l/q] > ") (flush)
  (some-> (read-line) str/trim str/lower-case))

(defn- tend-day!
  "1日ぶんの配分調整ループ。l/空行でロックイン(allocationを返す)、
   q/EOFで打ち切り(nilを返す)。"
  [day]
  (loop [allocation core/initial-allocation]
    (print-day! day allocation)
    (let [cmd (read-command!)
          transfer (when cmd (parse-transfer cmd))]
      (cond
        (nil? cmd) nil
        (= cmd "q") nil
        (or (= cmd "l") (= cmd "")) allocation
        transfer (let [[from to] transfer]
                   (recur (core/shift-allocation allocation from to step)))
        :else (do (println "dm/ds/md/ms/sd/sm(配点移動) / l(ock) / q(uit) のいずれかを入力してください。")
                  (recur allocation))))))

(defn- play-loop!
  "全日程を消化するまでtend-day!→resolve-dayを繰り返す。途中で
   nil(打ち切り)が返ったら、そこまでのstateで終える。"
  [days]
  (loop [state core/initial-state]
    (if (core/complete? state days)
      state
      (let [day (core/current-day state days)
            allocation (tend-day! day)]
        (if (nil? allocation)
          state
          (let [next-state (core/resolve-day state days allocation)]
            (println (format " -> %s (funds %+d, bond %+d)"
                              (name (last (:outcomes next-state)))
                              (:funds next-state)
                              (:family-bond next-state)))
            (recur next-state)))))))

(defn -main
  "Entry point for `clojure -M -m ghosthacker-family.terminal`."
  [& _args]
  (println "GHOST HACKER: FAMILY — opening-week")
  (println "各日のreceptionが最大になるよう配点を移動し、l でロックイン。")
  (println)
  (let [state (play-loop! days/opening-week)
        result (core/summary state)]
    (println)
    (println "=== RESULT ===")
    (println (format "grade=%s funds=%d family-bond=%d bond-level=%s"
                      (name (:grade result))
                      (:funds result)
                      (:family-bond result)
                      (name (:bond-level result))))))
