(ns cronj.test-core
  (:use midje.sweet)
  (:require [cronj.core :as cj]
            [cronj.data.timetable :as tt]
            [cronj.data.task :as tk]
            [cronj.data.timer :as tm] :reload))






(clojure.pprint/pprint (macroexpand-1 '(cj/defcronj cnj
                                         :interval 2
                                         :tasks [{:id       :t2
                                                  :handler  (fn [dt opts] (println dt opts))
                                                  :schedule "/2 * * * * * *"
                                                  :enabled  true
                                                  :opts     {:home "/home/cronj"}}

                                                 {:id       :t2
                                                  :handler  (fn [dt opts] (println dt opts))
                                                  :schedule "/2 * * * * * *"
                                                  :enabled  true
                                                  :opts     {:ex "example"}}])))



(def cnj (cj/cronj
          :interval 2
          :tasks [{:id       :t1
                   :handler  (fn [dt opts] (println dt opts))
                   :schedule "/2 * * * * * *"
                   :enabled  true
                   :opts     {:home "/home/cronj"}}

                  {:id       :t2
                   :handler  (fn [dt opts] (println dt opts))
                   :schedule "/2 * * * * * *"
                   :enabled  true
                   :opts     {:ex "example"}}]))

(cj/defcronj cnj
  :interval 2
  :entries [{:id       :t1
             :handler  (fn [dt opts] (println dt opts))
             :schedule "/2 * * * * * *"
             :enabled  true
             :opts     {:home "/home/cronj"}}

            {:id       :t2
             :handler  (fn [dt opts] (println dt opts))
             :schedule "/2 * * * * * *"
             :enabled  true
             :opts     {:ex "example"}}])

(println cnj)

(cj/start! cnj)

(cj/stop! cnj)

(def c (cj/cronj))

(tt/schedule-task (:timetable c) (tk/task :1 (fn [dt opts] (println dt opts))) "/2 * * * * * *")

(tm/start! (:timer c))
(tm/stop! (:timer c))

(comment
  (cj/unschedule-all-tasks!)
  (cj/load-tasks!
   [{:id 1 :desc 1 :handler #(println "Task 1: " %) :tab "0-60/2 * * * * * *"}])

  (cj/schedule-task! {:id 2 :desc 2 :handler #(println "Task 2: " %) :tab "1-60/2 * * * * * *"} )
  (println g/*timesheet*)
  (println g/*timeloop*)
  (.getWatches g/*timeloop*)

  (cj/start!)
  (cj/running?)
  (cj/stopped?)
  (cj/stop!)
  (println g/*timeloop*)

  (swap! g/*timeloop* assoc :last-check [2 3] :last-check-time (clj-time.core/now))
)
