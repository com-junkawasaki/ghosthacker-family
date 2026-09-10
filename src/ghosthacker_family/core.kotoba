(ns ghosthacker-family.core
  "GHOST HACKER: FAMILY -- listening-bar management-sim core (ADR-2607023200,
  portfolio title #5, Ren & Nei co-op).

  Pure, host-free judgment/state engine: Ren & Nei run the listening bar
  attached to the agency's office, day by day. Each day they allocate a
  fixed pool of `total-attention` points across three categories --
  `:drinks` (物販/ドリンク仕込み), `:music` (選曲/音響), `:staff-care`
  (常連・スタッフのケア) -- and that allocation is judged against the
  day's hidden `:need` (the distribution the day actually called for).
  Unlike FLOW/HARMONY (beat-grid timing) or TUNING (single-dial precision),
  FAMILY's single clear per-day input is a 3-way allocation, and the single
  clear per-day transition is `resolve-day` -- there is no elaborate
  economic simulation here (no prices, no inventory, no multi-day supply
  chains): one allocation in, one judged tier out, same modest scope every
  other title in this portfolio keeps to. The tier feeds two running
  totals -- `:funds` (can the bar survive) and `:family-bond` (is this
  found-family, `\"Family\"` off FreeTEMPO's *Life* (2010), actually
  strengthening) -- which is the point: the management-sim mechanics are
  the found-family theme, not decoration on top of it. No rendering or
  input I/O lives here -- those are host adapters layered on top, same
  split as every other title in this portfolio."
  )

(def total-attention
  "1日にRen&Neiが配分できるattention pointの総量(固定)。カテゴリ間で
   移動するだけで、増減はしない(shift-allocationが不変条件として保つ)。"
  10)

(def categories
  "配分先の3カテゴリ。"
  [:drinks :music :staff-care])

(def initial-allocation
  "既定の初期配分(合計はtotal-attentionに一致)。ドリンク多め・音楽/
   スタッフケアは均等寄りの、まだ『その日の空気』を読む前の暫定値。"
  {:drinks 4 :music 3 :staff-care 3})

(defn- magnitude
  "abs。core.cljcはJVM/CLJS両対応なので`Math/abs`/`js/Math.abs`を直接
   呼ばず、この純Clojure実装で済ませる(ghosthacker-tuning.core と同じ方針)。"
  [x]
  (if (neg? x) (- x) x))

(defn shift-allocation
  "allocationのfrom-catからto-catへamount分ポイントを移す。移動元の残量
   (get allocation from-cat)を超える移動は残量分に切り詰め、負のamountは
   0に切り詰める -- どちらの経路でも合計total-attentionという不変条件が
   常に保たれる(ghosthacker-tuning.core/nudge-dialのclamp01と同じ役割)。"
  [allocation from-cat to-cat amount]
  (let [amount (max 0 (min amount (get allocation from-cat 0)))]
    (-> allocation
        (update from-cat - amount)
        (update to-cat + amount))))

(def thriving-window
  "既定の:thriving判定窓(allocationとneedのカテゴリ別絶対差の合計)。"
  2)
(def steady-window
  "既定の:steady判定窓(同上)。これを超えたら:struggling。"
  6)

(defn mismatch
  "allocationとneed(どちらも{カテゴリ -> point}のマップ、合計は両方
   total-attention)の、カテゴリ別絶対差の合計を返す(0=完全一致、値が
   大きいほどその日の空気を読み違えている)。"
  [allocation need]
  (reduce + (map (fn [c] (magnitude (- (get allocation c 0) (get need c 0))))
                 categories)))

(defn judge-mismatch
  "mismatch(数値)から日次の判定tierを返す。"
  [m]
  (cond
    (<= m thriving-window) :thriving
    (<= m steady-window) :steady
    :else :struggling))

(defn judge-day
  "allocationをneedに対して判定する(mismatch + judge-mismatchの合成)。"
  [allocation need]
  (judge-mismatch (mismatch allocation need)))

(def initial-state
  {:funds 0
   :family-bond 0
   :day-index 0
   :outcomes []})

(def day-rewards
  "tierごとの:funds/:family-bond増分。:strugglingは両方マイナス --
   空気を読み違えた日は懐にも関係にも響く、という単純な対応関係。"
  {:thriving {:funds 300 :family-bond 3}
   :steady {:funds 100 :family-bond 1}
   :struggling {:funds -100 :family-bond -2}})

(defn apply-outcome
  "tierをstateに反映する。outcomesへ積み、funds/family-bondを更新し、
   day-indexを1つ進める。"
  [state tier]
  (let [{:keys [funds family-bond]} (get day-rewards tier)]
    (-> state
        (update :outcomes conj tier)
        (update :funds + funds)
        (update :family-bond + family-bond)
        (update :day-index inc))))

(defn current-day
  "days(ベクタ、各要素{:label :need})のうち、stateが現在向き合うべき日を
   返す。全日程消化後はnil。"
  [state days]
  (nth days (:day-index state) nil))

(defn complete?
  "state(daysに対する)が全日程を消化し終えたか。"
  [state days]
  (>= (:day-index state) (count days)))

(defn resolve-day
  "現在dayのneedに対してallocationを判定し、stateに適用する。全日程消化
   済みならstateをそのまま返す(呼び出し側の責任でcomplete?を先に
   チェックすること) -- ghosthacker-tuning.core/lock-inと同型の、この
   core唯一の日次state遷移関数。"
  [state days allocation]
  (if (complete? state days)
    state
    (let [need (:need (current-day state days))]
      (apply-outcome state (judge-day allocation need)))))

(defn bond-level
  "family-bondの累計から、found-family度合いのラベルを返す。"
  [state]
  (let [b (:family-bond state)]
    (cond
      (>= b 10) :found-family
      (>= b 4) :warming
      (>= b 0) :fragile
      :else :estranged)))

(defn grade
  "state(全日程消化後を想定)から最終評価を返す。fundsが赤字だと、
   family-bondが高くてもトップグレードにはならない(店が保たなければ
   意味がない、という単純な優先順位)。"
  [state]
  (let [b (:family-bond state)
        solvent? (>= (:funds state) 0)]
    (cond
      (and solvent? (>= b 10)) :s
      (>= b 10) :a
      (and solvent? (>= b 4)) :b
      (>= b 0) :c
      :else :d)))

(defn summary
  "runの結果サマリ。ホストアダプタ側のリザルト画面にそのまま渡せる形。"
  [state]
  {:funds (:funds state)
   :family-bond (:family-bond state)
   :bond-level (bond-level state)
   :grade (grade state)
   :day-count (count (:outcomes state))})

(defn play
  "daysに対し、allocationsを順にresolve-dayで適用する。allocationsが
   尽きても全日程終わっていなければ、そこまでのstateで打ち切る(ホスト
   アダプタが途中で中断した場合に相当)。"
  [days allocations]
  (loop [state initial-state
         as (seq allocations)]
    (if (or (complete? state days) (nil? as))
      state
      (recur (resolve-day state days (first as)) (next as)))))

(defn play-summary
  "play + summaryの合成。ホストアダプタが1run分のallocation列を録り
   終えた後に呼ぶ最短経路。"
  [days allocations]
  (summary (play days allocations)))
