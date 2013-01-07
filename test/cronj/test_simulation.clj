(ns cronj.test-simulation
  (:use midje.sweet
        hara.testing)
  (:require [clj-time.core :as t]
            [clj-time.local :as lt]
            [cronj.core :as cj]
            [cronj.simulation :as sm] :reload))

(def cnj (cj/cronj
          :entries [{:id       :t1
                     :handler  (fn [dt opts] (println "job 1" dt))
                     :schedule "/2 * * * * * *"
                     :enabled  true}

                    {:id       :t2
                     :handler  (fn [dt opts] (println "job 2" dt))
                     :schedule "/4 * * * * * *"
                     :enabled  true}]))

(time (sm/simulate-st cnj
                (lt/to-local-date-time (t/date-time 2000 1 1 1 1))
                (lt/to-local-date-time (t/date-time 2000 1 1 1 2))
                (t/secs 1)
                ))

(time (sm/simulate cnj
                (lt/to-local-date-time (t/date-time 2000 1 1 1 1))
                (lt/to-local-date-time (t/date-time 2000 1 1 1 2))
                (t/secs 1)))

;;(exec-st (first (:timetable cnj)) (t/date-time 2000 1 1 1))

;;(tt/trigger-time (:timetable cnj) (lt/to-local-date-time (t/date-time 2000 1 1 1 1)))

;;(:handler (:task (first (:timetable cnj))))
