(ns cronj.test-task
    (:use midje.sweet)
    (:require [hara.dyna :as d]
              [clj-time.local :as lt]
              [cronj.data.task :as t] :reload))

(facts "task creation"
  (let [mt (t/task :test-task "test-task" (fn [_]))]
    (fact "the created task is accepted as a task"
      (t/is-task?   mt) => true)
    (fact "the test is enabled by default"
      (t/enabled?   mt) => true)
    (fact "the test is therefore not by disabled"
      (t/disabled?  mt) => false) ;; expect that
    (fact "there are no tasks running"
      (t/running    mt) => ())  ;;
    (fact "there should be no last executed id"
      (t/last-exec  mt) => nil)
    (fact "there should be no last successful id"
      (t/last-successful mt) => nil)
    (fact "output print friendly form"
      (t/<# mt) => {:id :test-task
                    :desc "test-task"
                    :enabled true
                    :running ()
                    :last-exec nil
                    :last-successful nil})))


(facts "task execution"
  "Setup the data object as well as the tasks that manipulate the data object"
  (let [mda (atom nil)
        mt1 (t/task :mt1 "mt1" (fn [_] (reset! mda 1)))
        mt2 (t/task :mt2 "mt2" (fn [_] (reset! mda 2)))]

    (do (t/exec! mt1 :mt1-first) (Thread/sleep 5))      ;; time for thread to update
    (facts "mt1-first"
      (fact "data should be 1"
        (deref mda) => 1)
      (fact "last-exec should be :mt1-first"
        (t/last-exec mt1) => :mt1-first)
      (fact "last-successful should be :mt1-first"
        (t/last-successful mt1) => :mt1-first))

    (do (t/exec! mt2 :mt2-first) (Thread/sleep 5))     ;; time for thread to update
    (facts "atfer mt2-first"
      (fact "data should be 2"
        (deref mda) => 2)
      (fact "last-exec should be :mt2-first"
        (t/last-exec mt2) => :mt2-first)
      (fact "last-successful should be :mt2-first"
        (t/last-successful mt2) => :mt2-first))

    (do (t/exec! mt1 :mt1-second) (Thread/sleep 5))    ;; time for thread to update
    (facts "after mt1-second"
      (fact "data should be 1"
        (deref mda) => 1)
      (fact "last-exec should be :mt1-second"
        (t/last-exec mt1) => :mt1-second)
      (fact "last-successful should be :mt1-second"
        (t/last-successful mt1) => :mt1-second))))




(comment



  (def job-100ms (t/task :1 "100ms" (fn [_] (println "100ms Job")  (Thread/sleep 100))))
  (def job-200ms (t/task :2 "200ms" (fn [_] (println "200ms Job")  (Thread/sleep 200))))
  (def job-200-args (t/task :2 "200ms" (fn [_ a b] (println "200ms Job" a b)  (Thread/sleep 200)) :a " A " :b " B "))


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

  (def a (t/task :1 "1st task" (fn [_] (println "Hello There")  (Thread/sleep 10000))))
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
  )
