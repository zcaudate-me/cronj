(ns cronj.test-core
  (:use midje.sweet
        clojure.pprint)
  (:require [hara.dyna :as d]
            [hara.iotam :as i]
            [cronj.global :as g]
            [cronj.loop :as lp]
            [cronj.core :as cj]  :reload))

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
