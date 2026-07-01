(ns kotoba.robotics.export-test
  (:require [clojure.test :refer [deftest is testing]]
            [kotoba.robotics :as rob]
            [kotoba.robotics.export :as ex]))

(def missions [(rob/mission "M1" "robot-1" "deliver parcel")])
(def actions [(rob/action "A1" "M1" :move :low)
              (rob/action "A2" "M1" :grasp :safety-critical)])

(deftest csv-export
  (testing "missions CSV header + row"
    (let [csv (ex/missions->csv missions)]
      (is (re-find #"mission_id,robot,objective,status" csv))
      (is (re-find #"M1,robot-1,deliver parcel,planned" csv))))
  (testing "actions CSV records governor gate"
    (let [csv (ex/actions->csv actions #{:none :low :medium :safety-critical})]
      (is (re-find #"action_id,mission,kind,safety" csv))
      (is (re-find #"A1,M1,move,low" csv))
      (is (re-find #"yes" csv))  ; actuates_hardware
      (is (re-find #"require_sign_off|requires_sign_off" csv)))))

(deftest json-export
  (testing "missions JSON"
    (let [j (ex/missions->json missions)]
      (is (re-find #"\"mission_id\":\"M1\"" j))
      (is (re-find #"\"status\":\"planned\"" j))))
  (testing "actions JSON carries boolean fields and gate"
    (let [j (ex/actions->json actions #{:none :low :medium :safety-critical})]
      (is (re-find #"\"actuates_hardware\":true" j))
      (is (re-find #"\"requires_sign_off\":true" j))
      (is (re-find #"\"gate\":\"" j)))))
