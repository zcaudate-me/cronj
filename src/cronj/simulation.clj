(ns cronj.simulation
  (:require [clj-time.core :as t]
            [cronj.data.timetable :as tt]))

(defn simulate [cnj start end interval & [pause]]
  (if-not (t/before? end start)
    (do
      (tt/trigger-time (:timetable cnj) start)
      (if pause (Thread/sleep pause))
      (recur cnj (t/plus start interval) end interval pause))))

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

(defn simulate-st [cnj start end interval & [pause]]
  (if-not (t/before? end start)
    (do
      (doseq [t (:timetable cnj)]
        (exec-st t start))
      (if pause (Thread/sleep pause))
      (recur cnj (t/plus start interval) end interval pause))))
