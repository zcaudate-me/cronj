(ns cronj.test-core
  (:use midje.sweet
        clojure.pprint)
    (:require [hara.data.dyna :as d]
              [cronj.core :as cj] :reload))

(println cj/*cronj*)

(cj/unschedule-all-tasks!)
(cj/load-tasks!
 [{:id 1 :desc 1 :handler #(println "Task 1: " %) :tab "0-60/2 * * * * * *"}])

(cj/schedule-task! {:id 2 :desc 2 :handler #(println "Task 2: " %) :tab "1-60/2 * * * * * *"} )
(println cj/*cronj*)
(cj/start!)
(cj/running?)
(cj/stopped?)
(cj/stop!)
