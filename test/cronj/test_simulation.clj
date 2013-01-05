(ns cronj.test-simulation
  (:use midje.sweet
        hara.testing)
  (:require [clj-time.core :as t]
            [clj-time.local :as lt]
            [cronj.core :as cj]
            [cronj.data.timetable :as tt]
            [cronj.data.task :as tk]
            [cronj.data.timer :as tm] :reload))

(def cnj (cj/cronj
          :entries [{:id       :t1
                     :handler  (fn [dt opts] (println "job 1" dt))
                     :schedule "* * * * * * *"
                     :enabled  true}

                    {:id       :t2
                     :handler  (fn [dt opts] (println "job 2" dt))
                     :schedule "* * * * * * *"
                     :enabled  true}]))

(tt/trigger-time (:timetable cnj) (lt/to-local-date-time (t/date-time 2000 1 1 1 1)))

(:handler (:task (first (:timetable cnj))))

(defn simulate [cnj start end interval & [pause]]
  (if-not (t/before? end start)
    (do
      (tt/trigger-time (:timetable cnj) start)
      (if pause (Thread/sleep pause))
      (recur cnj (t/plus start interval) end interval pause))))

(defn exec-st [task dt]
  (let [pre       (:pre-hook task)
        post      (:post-hook task)
        handler   (:handler task)
        opts      (or (:opts task) {})
        exec-hook (fn [hook dt opts]
                    (if (fn? hook) (hook dt opts) opts))
        fopts  (exec-hook pre dt opts)
        result    (handler dt fopts)]
    (exec-hook post dt (assoc fopts :result result))))

(defn simulate-st [cnj start end interval & [pause]]
  (if-not (t/before? end start)
    (do
      (doseq [t (:timetable cnj)]
        (exec-st t start))
      (if pause (Thread/sleep pause))
      (recur cnj (t/plus start interval) end interval pause)))
  )

(time (simulate-st cnj
                (lt/to-local-date-time (t/date-time 2000 1 1 1))
                (lt/to-local-date-time (t/date-time 2000 1 1 2))
                (t/secs 1)))

(time (simulate cnj
                (lt/to-local-date-time (t/date-time 2000 1 1 1))
                (lt/to-local-date-time (t/date-time 2000 1 1 2))
                (t/secs 1)))
