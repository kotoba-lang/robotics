(ns kotoba.robotics.export
  "Operator-facing export for a robotics actor.

  Renders missions and actions to CSV and JSON for safety audit export and
  downstream reporting. Pure data → text: no network, no I/O. Exports are
  read-only evidence the audit ledger can append; they never dispatch."
  (:require [clojure.string :as str]
            [kotoba.robotics :as rob]))

(defn- csv-cell [v]
  (let [s (str (if (nil? v) "" v))]
    (if (re-find #"[\",\n]" s)
      (str "\"" (str/replace s "\"" "\"\"") "\"")
      s)))

(defn- csv-row [vals] (str/join "," (map csv-cell vals)))

(defn- json-str [v]
  (-> (str (if (nil? v) "" v))
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")
      (str/replace "\n" "\\n")))

(defn missions->csv [missions]
  (str/join "\n"
    (cons (csv-row ["mission_id" "robot" "objective" "status"])
          (for [m missions]
            (csv-row [(:mission/id m)
                      (:mission/robot m)
                      (:mission/objective m)
                      (name (or (:mission/status m) :planned))])))))

(defn actions->csv
  "Export actions to CSV. `allowed` is the safety-class set the governor
  permitted; the `gate` column records the governor's decision per action."
  ([actions] (actions->csv actions #{:none :low :medium}))
  ([actions allowed]
   (let [gate (fn [a]
                (let [g (rob/gate a allowed)]
                  (name (:gate/decision g))))]
     (str/join "\n"
       (cons (csv-row ["action_id" "mission" "kind" "safety" "actuates_hardware" "requires_sign_off" "gate"])
             (for [a actions]
               (csv-row [(:action/id a)
                         (:action/mission a)
                         (name (:action/kind a))
                         (name (:action/safety a))
                         (if (rob/actuates-hardware? a) "yes" "no")
                         (if (rob/requires-sign-off? a) "yes" "no")
                         (gate a)])))))))

(defn missions->json [missions]
  (str "["
       (str/join ","
                 (for [m missions]
                   (str "{\"mission_id\":\"" (json-str (:mission/id m)) "\","
                        "\"robot\":\"" (json-str (:mission/robot m)) "\","
                        "\"objective\":\"" (json-str (:mission/objective m)) "\","
                        "\"status\":\"" (name (or (:mission/status m) :planned)) "\"}")))
       "]"))

(defn actions->json
  ([actions] (actions->json actions #{:none :low :medium}))
  ([actions allowed]
   (str "["
        (str/join ","
                  (for [a actions]
                    (let [g (rob/gate a allowed)]
                      (str "{\"action_id\":\"" (json-str (:action/id a)) "\","
                           "\"mission\":\"" (json-str (:action/mission a)) "\","
                           "\"kind\":\"" (name (:action/kind a)) "\","
                           "\"safety\":\"" (name (:action/safety a)) "\","
                           "\"actuates_hardware\":" (if (rob/actuates-hardware? a) "true" "false") ","
                           "\"requires_sign_off\":" (if (rob/requires-sign-off? a) "true" "false") ","
                           "\"gate\":\"" (name (:gate/decision g)) "\"}"))))
        "]")))
