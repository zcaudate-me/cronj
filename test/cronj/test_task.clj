(ns cronj.test-task
    (:use midje.sweet)
    (:require [hara.data.dyna :as d]
              [cronj.task :as t] :reload))

(def job-100ms (cronj.task/new :1 "100ms" (fn [_] (println "100ms Job")  (Thread/sleep 100))))
(def job-200ms (cronj.task/new :2 "200ms" (fn [_] (println "200ms Job")  (Thread/sleep 200))))
(def job-200-args (cronj.task/new :2 "200ms" (fn [_ a b] (println "200ms Job" a b)  (Thread/sleep 200)) :a " A " :b " B "))



(do
  (t/exec! job-100ms :test)
  (Thread/sleep 50)
  (fact "This will return"
    (t/running job-100ms) => '(:test))
  (Thread/sleep 200)
  (fact "This will return empty"
    (t/running job-100ms) => '()))

(fact "Testing enable and disable functions"
  (t/enable job-200ms) => t/enabled?
  (t/disable job-200ms) => t/disabled?)


(def a (cronj.task/new :1 "1st task" (fn [_] (println "Hello There")  (Thread/sleep 10000))))
;;(#'t/exec-fn a 1 (fn [_] (Thread/sleep 10000) (println "Hello There")) {})
;;(#'t/register-thread a 2 (future (println 111)))
;;(t/enabled? a)
;;(t/disabled? a)
;;(t/exec! a 1)
;;(t/kill! a 1)
;;(t/kill-all! a)
;;(t/running a)
;;(println a)
;;((:handler a) 1)



#_(fact "new creates a new task"
  )
