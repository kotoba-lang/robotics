(ns kotoba.robotics.export-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
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

(deftest csv-export-quotes-a-bare-carriage-return
  ;; RFC 4180 requires quoting a field containing CR, LF, or a comma --
  ;; \r alone is also a line terminator every standard CSV reader
  ;; recognizes, but the check here only ever covered \n. Verified
  ;; against Python's csv module: an unquoted bare \r split the row into
  ;; two corrupted rows on read-back.
  (let [m [(rob/mission "M1" "robot-1" (str "deliver" (char 13) "parcel"))]
        csv (ex/missions->csv m)]
    (is (str/includes? csv "\"deliver\rparcel\""))))

(deftest json-export-escapes-every-c0-control-character
  ;; RFC 8259 requires EVERY control character U+0000-U+001F to be
  ;; escaped, not just \\ \" and \n -- an operator-supplied objective
  ;; containing a raw \t or other control byte would otherwise be copied
  ;; through raw, producing invalid JSON (verified against Python's
  ;; strict json module).
  (let [m [(rob/mission "M1" "robot-1" (str "objective with" (char 9) "tab and" (char 1) "control char"))]
        j (ex/missions->json m)]
    (is (str/includes? j "objective with\\ttab and\\u0001control char"))))
