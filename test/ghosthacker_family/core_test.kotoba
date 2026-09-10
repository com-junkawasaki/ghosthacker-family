(ns ghosthacker-family.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [ghosthacker-family.core :as core]))

(deftest shift-allocation-clamps
  (testing "移動量は移動元の残量に、負の指定は0にclampされる(合計は不変)"
    (is (= {:drinks 0 :music 7 :staff-care 3}
           (core/shift-allocation {:drinks 4 :music 3 :staff-care 3} :drinks :music 10)))
    (is (= {:drinks 4 :music 3 :staff-care 3}
           (core/shift-allocation {:drinks 4 :music 3 :staff-care 3} :drinks :music -5)))
    (is (= {:drinks 3 :music 4 :staff-care 3}
           (core/shift-allocation {:drinks 4 :music 3 :staff-care 3} :drinks :music 1)))))

(deftest judge-mismatch-tiers
  (testing "thriving/steady/strugglingの境界"
    (is (= :thriving (core/judge-mismatch 0)))
    (is (= :thriving (core/judge-mismatch core/thriving-window)))
    (is (= :steady (core/judge-mismatch (inc core/thriving-window))))
    (is (= :steady (core/judge-mismatch core/steady-window)))
    (is (= :struggling (core/judge-mismatch (inc core/steady-window))))
    (is (= :struggling (core/judge-mismatch 20)))))

(deftest judge-day-computes-mismatch
  (testing "allocationとneedの絶対差合計から判定する"
    (is (= :thriving (core/judge-day {:drinks 5 :music 3 :staff-care 2}
                                      {:drinks 5 :music 3 :staff-care 2})))
    (is (= :steady (core/judge-day {:drinks 8 :music 1 :staff-care 1}
                                    {:drinks 5 :music 3 :staff-care 2})))
    (is (= :struggling (core/judge-day {:drinks 10 :music 0 :staff-care 0}
                                        {:drinks 5 :music 3 :staff-care 2})))))

(deftest apply-outcome-state
  (testing ":strugglingはfunds/family-bondを共に減らす"
    (let [after-thriving (core/apply-outcome core/initial-state :thriving)
          after-struggling (core/apply-outcome after-thriving :struggling)]
      (is (= 300 (:funds after-thriving)))
      (is (= 3 (:family-bond after-thriving)))
      (is (= 200 (:funds after-struggling)))
      (is (= 1 (:family-bond after-struggling)))
      (is (= 2 (:day-index after-struggling)))
      (is (= [:thriving :struggling] (:outcomes after-struggling))))))

(def ^:private days
  [{:label :a :need {:drinks 5 :music 3 :staff-care 2}}
   {:label :b :need {:drinks 4 :music 4 :staff-care 2}}
   {:label :c :need {:drinks 2 :music 3 :staff-care 5}}])

(deftest current-day-and-complete
  (testing "day-indexに応じてcurrent-day/complete?が正しく動く"
    (is (= {:label :a :need {:drinks 5 :music 3 :staff-care 2}}
           (core/current-day core/initial-state days)))
    (is (not (core/complete? core/initial-state days)))
    (let [s3 (assoc core/initial-state :day-index 3)]
      (is (nil? (core/current-day s3 days)))
      (is (core/complete? s3 days)))))

(deftest resolve-day-judges-current-day
  (testing "resolve-dayは現在dayのneedに対して判定し、次へ進む"
    (let [s1 (core/resolve-day core/initial-state days {:drinks 5 :music 3 :staff-care 2})]
      (is (= :thriving (last (:outcomes s1))))
      (is (= 1 (:day-index s1)))
      ;; s1の現在dayはb(need 4/4/2)。10/0/0で大きく外す -> :struggling
      (let [s2 (core/resolve-day s1 days {:drinks 10 :music 0 :staff-care 0})]
        (is (= :struggling (last (:outcomes s2))))
        (is (= 2 (:day-index s2)))))))

(deftest resolve-day-no-op-when-complete
  (testing "全日程消化済みならresolve-dayはstateをそのまま返す"
    (let [done (assoc core/initial-state :day-index (count days))]
      (is (= done (core/resolve-day done days {:drinks 5 :music 3 :staff-care 2}))))))

(deftest bond-level-and-grade
  (testing "bond-levelはfamily-bondの累計で決まる"
    (is (= :found-family (core/bond-level (assoc core/initial-state :family-bond 10))))
    (is (= :warming (core/bond-level (assoc core/initial-state :family-bond 4))))
    (is (= :fragile (core/bond-level (assoc core/initial-state :family-bond 0))))
    (is (= :estranged (core/bond-level (assoc core/initial-state :family-bond -1)))))
  (testing "gradeはfamily-bond閾値 + funds黒字/赤字で決まる(赤字だとトップグレードにならない)"
    (is (= :s (core/grade (assoc core/initial-state :family-bond 10 :funds 0))))
    (is (= :a (core/grade (assoc core/initial-state :family-bond 10 :funds -1))))
    (is (= :b (core/grade (assoc core/initial-state :family-bond 4 :funds 0))))
    (is (= :c (core/grade (assoc core/initial-state :family-bond 0 :funds -1))))
    (is (= :d (core/grade (assoc core/initial-state :family-bond -1 :funds -1))))))

(deftest play-and-play-summary
  (testing "playは全日程thrivingすると終了する"
    (let [state (core/play days [{:drinks 5 :music 3 :staff-care 2}
                                  {:drinks 4 :music 4 :staff-care 2}
                                  {:drinks 2 :music 3 :staff-care 5}])]
      (is (core/complete? state days))
      (is (= [:thriving :thriving :thriving] (:outcomes state)))))
  (testing "allocationsが尽きても未完走ならそこまでのstateを返す"
    (let [state (core/play days [{:drinks 5 :music 3 :staff-care 2}])]
      (is (not (core/complete? state days)))
      (is (= 1 (:day-index state)))))
  (testing "play-summaryはplay+summaryの合成"
    (let [summary (core/play-summary days [{:drinks 5 :music 3 :staff-care 2}
                                            {:drinks 4 :music 4 :staff-care 2}
                                            {:drinks 2 :music 3 :staff-care 5}])]
      (is (= :b (:grade summary)))
      (is (= 900 (:funds summary)))
      (is (= 9 (:family-bond summary)))
      (is (= :warming (:bond-level summary)))
      (is (= 3 (:day-count summary))))))
