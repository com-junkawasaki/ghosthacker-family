(ns ghosthacker-family.terminal-test
  "-mainそのものはテストせず、private var経由でreception-pct/tend-day!/
   play-loop!を直接叩く。実プロセスとしての-main自体は手動検証済み
   （完走/q途中打ち切り/不正コマンドの再入力要求、いずれも正しく完了し
   プロセスがハングしないことを確認）。

   すべての値が整数(attention point/funds/family-bond)なので、
   ghosthacker-tuning.terminal-testのようなfloat epsilon比較
   (close-to?)は不要 -- `=`の厳密一致で十分。"
  (:require [clojure.test :refer [deftest is testing]]
            [ghosthacker-family.core :as core]
            [ghosthacker-family.days :as days]
            [ghosthacker-family.terminal :as terminal]))

(def ^:private reception-pct #'terminal/reception-pct)
(def ^:private tend-day! #'terminal/tend-day!)
(def ^:private play-loop! #'terminal/play-loop!)

(defn- silently [thunk]
  (binding [*out* (java.io.StringWriter.)]
    (thunk)))

(deftest reception-pct-boundary-test
  (testing "ぴったりなら100%、mismatchが最大(2*total-attention)なら0%"
    (is (= 100 (reception-pct {:drinks 5 :music 3 :staff-care 2}
                              {:drinks 5 :music 3 :staff-care 2})))
    (is (= 50 (reception-pct {:drinks 10 :music 0 :staff-care 0}
                             {:drinks 5 :music 3 :staff-care 2})))
    (is (= 0 (reception-pct {:drinks 10 :music 0 :staff-care 0}
                            {:drinks 0 :music 10 :staff-care 0})))))

(deftest tend-day-lock-and-quit-test
  (let [day {:label :test :need {:drinks 4 :music 3 :staff-care 3}}]
    (testing "2文字コマンド(from+to)でshiftし、lでその時点のallocationをロックインする"
      (let [allocation (silently #(with-in-str "dm\ndm\nl\n" (tend-day! day)))]
        (is (= {:drinks 2 :music 5 :staff-care 3} allocation))))
    (testing "空行でもロックインできる"
      (let [allocation (silently #(with-in-str "dm\n\n" (tend-day! day)))]
        (is (= {:drinks 3 :music 4 :staff-care 3} allocation))))
    (testing "qまたはEOFで打ち切り(nil)"
      (is (nil? (silently #(with-in-str "q\n" (tend-day! day)))))
      (is (nil? (silently #(with-in-str "dm\n" (tend-day! day))))))
    (testing "不正コマンド(長さ違い/同一カテゴリ)は読み飛ばし、次の有効なコマンドを処理する"
      (let [allocation (silently #(with-in-str "xyz\ndd\nl\n" (tend-day! day)))]
        (is (= core/initial-allocation allocation))))))

(deftest play-loop-eof-boundary-test
  (testing "全日程を完璧にロックインし切れば完走する"
    (let [two-days [{:label :a :need core/initial-allocation}
                    {:label :b :need core/initial-allocation}]
          state (silently #(with-in-str "l\nl\n" (play-loop! two-days)))]
      (is (core/complete? state two-days))
      (is (= [:thriving :thriving] (:outcomes state)))))
  (testing "途中でq/EOFになれば、そこまでのstateで打ち切る(未完走)"
    (let [state (silently #(with-in-str "l\nq\n" (play-loop! days/opening-week)))]
      (is (not (core/complete? state days/opening-week)))
      (is (= 1 (:day-index state))))))
