(ns kotoba.robotics.ui
  "Operator-facing safety console for a robotics actor.

  Renders an HTML read-only panel of robot missions and the governor's
  action gate decisions, using kotoba-lang/html + css. Pure data → markup:
  no network, no DOM. The governor gates hardware dispatch; this view only
  observes missions, actions and sign-off state — it never dispatches."
  (:require [html.core :as html]
            [css.core :as css]
            [kotoba.robotics :as rob]))

(def ^:private sheet
  {:rules
   {"body"              {:font-family "system-ui,-apple-system,sans-serif"
                        :margin 0 :color "#1a1a1a" :background "#fafafa"}
    "header.bar"        {:display :flex :align-items :center :gap 12
                        :padding "12px 20px" :background "#fff"
                        :border-bottom "1px solid #e5e5e5"}
    "header.bar h1"     {:font-size 18 :margin 0 :font-weight 600}
    "header.bar .badge" {:margin-left :auto :font-size 12 :color "#666"}
    "main"              {:max-width 980 :margin "24px auto" :padding "0 20px"}
    ".card"             {:background "#fff" :border "1px solid #e5e5e5"
                        :border-radius 8 :padding 16 :margin-bottom 16}
    "h2"                {:margin-top 0 :font-size 15}
    "table"             {:width "100%" :border-collapse :collapse :font-size 14}
    "th, td"            {:text-align :left :padding "8px 10px"
                        :border-bottom "1px solid #f0f0f0"}
    "th"                {:font-weight 600 :color "#555" :font-size 12
                        :text-transform :uppercase :letter-spacing "0.04em"}
    ".ok"               {:color "#137a3f"}
    ".warn"             {:color "#b25c00" :background "#fff8e1"
                        :padding "2px 6px" :border-radius 4}
    ".err"              {:color "#b3261e" :background "#fbe9e7"
                        :padding "2px 6px" :border-radius 4}
    ".critical"         {:color "#fff" :background "#b3261e"
                        :padding "2px 6px" :border-radius 4 :font-weight 600}
    ".muted"            {:color "#888"}}})

(defn- stylesheet [] (html/->html (css/style-node sheet)))

(defn safety-class [s]
  (cond
    (= s :safety-critical) :critical
    (contains? #{:high} s) :err
    (contains? #{:medium} s) :warn
    :else :ok))

(defn- gate-badge [decision]
  (case (:gate/decision decision)
    :permit          [:span.ok "permit"]
    :deny            [:span.err (str "deny · " (:gate/reason decision "—"))]
    :require-sign-off [:span.warn (str "sign-off · " (name (:gate/safety decision)))]
    [:span.err "invalid"]))

(defn missions-table [missions]
  [:table
   [:thead [:tr [:th "Mission"] [:th "Robot"] [:th "Objective"] [:th "Status"]]]
   [:tbody (for [m missions]
             [:tr [:td (:mission/id m)]
                  [:td (:mission/robot m)]
                  [:td (:mission/objective m)]
                  [:td (name (:mission/status m))]])]])

(defn actions-table [actions allowed]
  [:table
   [:thead [:tr [:th "Action"] [:th "Kind"] [:th "Safety"] [:th "Gate"]]]
   [:tbody
    (for [a actions]
      (let [dec (rob/gate a allowed)]
        [:tr
         [:td (:action/id a)]
         [:td (name (:action/kind a))]
         [:td {:class [(safety-class (:action/safety a))]} (name (:action/safety a))]
         [:td (gate-badge dec)]]))]])

(defn dashboard
  "Render a full HTML safety console page for a robotics operator."
  [{:keys [missions actions allowed-safety] :as ctx
    :or {allowed-safety #{:none :low :medium}}}]
  (html/->html
    [:html
     [:head [:meta {:charset "utf-8"}] [:title "cloud-itonami · robotics"]
      [:hiccup/raw (stylesheet)]]
     [:body
      [:header.bar
       [:h1 "Robotics Safety — Operator Console"]
       [:span.badge "read-only · governor-gated · never dispatches"]]
      [:main
       [:section.card [:h2 "Missions"] (missions-table missions)]
       (when (seq actions)
         [:section.card [:h2 "Action gate"]
          [:p.muted (str "allowed safety classes: "
                         (clojure.string/join ", " (map name (sort allowed-safety))))]
          (actions-table actions allowed-safety)])]]]))
