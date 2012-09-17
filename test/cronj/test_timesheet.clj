(ns cronj.test-timesheet
    (:use midje.sweet)
    (:require [hara.data.dyna :as d]
              [cronj.timesheet :as ts] :reload))

(def sheet (cronj.timesheet/new))

(d/empty! sheet)
(ts/schedule! sheet 0 0 #(println "job 0:" %) "/5 * * * * * *")
(ts/schedule! sheet 1 1 (fn [_] (Thread/sleep 100000)) "/5 * * * * * *")
(ts/trigger-task! sheet 1 1)
(ts/list-running sheet 1)
(println
 (ts/<all sheet))
(println
 (d/ids sheet))
