(ns cronj.simulation
  (:require [hara.ova :as v]
            [clj-time.core :as t]
            [cronj.data.tab :as tab]
            [cronj.data.timetable :as tt]))

;; Speed Up Execution.

(defn- simulate-loop [cnj start end interval pause]
  (if-not (t/before? end start)
    (do
      (tt/signal-tick (:timetable cnj) start)
      (if pause (Thread/sleep pause))
      (recur cnj (t/plus start interval) end interval pause))))

(defn simulate [cnj start end & [interval pause]]
  (let [interval (or interval (t/secs 1))
        pause    (or pause 0)]
    (simulate-loop cnj start end interval pause)))

;; Single Threaded Simulation.

(defn exec-st [task dt]
  (let [pre       (:pre-hook task)
        post      (:post-hook task)
        handler   (:handler (:task task))
        opts      (or (:opts task) {})
        exec-hook (fn [hook dt opts]
                    (if (fn? hook) (hook dt opts) opts))
        fopts  (exec-hook pre dt opts)
        result (handler dt fopts)]
    (exec-hook post dt (assoc fopts :result result))))

(defn- simulate-st-loop [cnj start end interval pause]
  (if-not (t/before? end start)
    (let [dt-array (tab/to-dt-array start)]
      (doseq [entry (v/select (:timetable cnj) [:enabled true])]
        (if (tab/match-array? dt-array (:tab-array entry))
          (exec-st entry start)))
      (if pause (Thread/sleep pause))
      (recur cnj (t/plus start interval) end interval pause))))

(defn simulate-st [cnj start end & [interval pause]]
  (let [interval (or interval (t/secs 1))
        pause    (or pause 0)]
    (simulate-st-loop cnj start end interval pause)))
