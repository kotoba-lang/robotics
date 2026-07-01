(ns kotoba.robotics-test
  (:require [clojure.test :refer [deftest is testing]]
            [kotoba.robotics :as rob]))

(deftest mission-test
  (is (= :planned (:mission/status (rob/mission "M1" "robot-1" "deliver parcel"
                                                 :boundaries {:geo "site-A"} :max-steps 100)))))

(deftest action-test
  (testing "valid move action"
    (let [a (rob/action "A1" "M1" :move :low :params {:to [0 1]})]
      (is (= :move (:action/kind a)))
      (is (rob/actuates-hardware? a))
      (is (not (rob/requires-sign-off? a)))))
  (testing "sense is not hardware"
    (is (not (rob/actuates-hardware? (rob/action "A1" "M1" :sense :none)))))
  (testing "safety-critical requires sign-off"
    (is (rob/requires-sign-off? (rob/action "A1" "M1" :grasp :safety-critical))))
  (testing "unknown kind or safety returns nil"
    (is (nil? (rob/action "A1" "M1" :teleport :low)))
    (is (nil? (rob/action "A1" "M1" :move :extreme)))))

(deftest safety-stop-test
  (is (= :e-stop (:stop/reason (rob/safety-stop "M1" :e-stop :source "operator"))))
  (is (nil? (rob/safety-stop "M1" :for-fun))))

(deftest telemetry-proof-test
  (let [p (rob/telemetry-proof "M1" :lidar {:range 3.2} :timestamp "2026-07-01T00:00Z")]
    (is (= :lidar (:proof/sensor p)))
    (is (= 3.2 (get-in p [:proof/reading :range])))))

(deftest gate-test
  (testing "permits a low-safety move when :low allowed"
    (let [a (rob/action "A1" "M1" :move :low)]
      (is (= :permit (:gate/decision (rob/gate a #{:none :low}))))))
  (testing "denies a safety class not in allowed set"
    (let [a (rob/action "A1" "M1" :move :high)]
      (is (= :deny (:gate/decision (rob/gate a #{:none :low}))))))
  (testing "safety-critical requires sign-off even when allowed"
    (let [a (rob/action "A1" "M1" :grasp :safety-critical)]
      (is (= :require-sign-off (:gate/decision (rob/gate a #{:safety-critical}))))))
  (testing "invalid input"
    (is (= :invalid (:gate/decision (rob/gate "x" #{}))))))
