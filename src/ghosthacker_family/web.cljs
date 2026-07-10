(ns ghosthacker-family.web
  "GHOST HACKER: FAMILY -- browser host adapter (ADR-2607023200 /
  ADR-2607100900 follow-up (b) host-adapter decision). Plain reagent, no
  Web Audio -- FAMILY has no real-time beat to track (a management-sim
  read-the-room loop, not Ren's reflex titles), so a button-driven
  shift/lock UI is the whole host, same low-complexity end of the
  spectrum as ECHOES/TUNING.

  Same input/judgment shape as ghosthacker_family/terminal.clj: each of
  the three transfer-arrow rows moves one attention point between two
  categories via core/shift-allocation, lock commits the current day's
  judgment via core/resolve-day, and a 'reception' percentage (proximity,
  no direction) is the only feedback -- rendered to the DOM instead of
  stdout."
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [ghosthacker-family.core :as core]
            [ghosthacker-family.days :as days]))

(def ^:private step 1)

(defn- reception-pct
  "allocationとneedのmismatchを0(遠い)〜100(ぴったり)%のreceptionに変換
  する(terminal.clj/reception-pctと同じ計算)。"
  [allocation need]
  (let [m (core/mismatch allocation need)
        worst (* 2 core/total-attention)]
    (js/Math.round (* 100 (- 1 (/ (min m worst) worst))))))

(defonce state
  (r/atom {:phase :playing        ; :playing | :result
           :days days/opening-week
           :allocation core/initial-allocation
           :game-state core/initial-state
           :last-outcome nil}))

(defn- shift! [from to]
  (swap! state update :allocation #(core/shift-allocation % from to step)))

(defn- lock-in! []
  (let [{:keys [days allocation game-state]} @state
        next-gs (core/resolve-day game-state days allocation)
        outcome (last (:outcomes next-gs))]
    (swap! state assoc
           :game-state next-gs
           :last-outcome outcome
           :allocation core/initial-allocation
           :phase (if (core/complete? next-gs days) :result :playing))))

(defn- restart! []
  (reset! state {:phase :playing
                 :days days/opening-week
                 :allocation core/initial-allocation
                 :game-state core/initial-state
                 :last-outcome nil}))

(def ^:private transfer-rows
  [[:drinks :music "drinks" "music"]
   [:drinks :staff-care "drinks" "staff-care"]
   [:music :staff-care "music" "staff-care"]])

(defn- transfer-row [[a b a-label b-label]]
  [:div.family-row {:key (str a b) :class (str "family-row-" (name a) "-" (name b))}
   [:button.family-shift-left {:on-click #(shift! b a)} "←"]
   [:span.family-row-label (str a-label " / " b-label)]
   [:button.family-shift-right {:on-click #(shift! a b)} "→"]])

(defn- playing-screen []
  (let [{:keys [days game-state allocation last-outcome]} @state
        day (core/current-day game-state days)]
    [:div.family-app
     [:h1 "GHOST HACKER: FAMILY"]
     [:p.family-sub "opening-week"]
     [:div.family-day (str "-- " (name (:label day)) " --")]
     [:div.family-reception (str "reception: " (reception-pct allocation (:need day)) "%")]
     [:div.family-allocation
      (str "drinks " (:drinks allocation)
           " / music " (:music allocation)
           " / staff-care " (:staff-care allocation))]
     (into [:div.family-controls] (map transfer-row transfer-rows))
     [:button.family-lock {:on-click lock-in!} "LOCK"]
     (when last-outcome [:div.family-outcome (name last-outcome)])
     [:p.family-hint (str (:day-index game-state) "/" (count days) " days tended")]]))

(defn- result-screen []
  (let [summary (core/summary (:game-state @state))]
    [:div.family-app
     [:h1 "GHOST HACKER: FAMILY"]
     [:h2 (str "grade: " (name (:grade summary)))]
     [:p (str "funds " (:funds summary) " / family-bond " (:family-bond summary))]
     [:p (str "bond-level: " (name (:bond-level summary)))]
     [:button.family-restart {:on-click restart!} "もう一度"]]))

(defn app []
  (case (:phase @state)
    :result [result-screen]
    [playing-screen]))

(defn ^:export mount []
  (when-let [el (.getElementById js/document "app")]
    (rdom/render [app] el)))

(defn ^:export init [] (mount))
