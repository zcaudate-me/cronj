(ns cronj.test-simulation
  (:use midje.sweet
        hara.checkers)
  (:require [clj-time.core :as t]
            [clj-time.local :as lt]
            [ova.core :as v]
            [cronj.core :as cj]
            [cronj.data.scheduler :as ts]
            [cronj.simulation :as sm] :reload))

;; Test for

(def ^:dynamic *holder* (atom []))
(def ^:dynamic *t1* (lt/to-local-date-time (t/date-time 2000 1 1 1 1)))
(def ^:dynamic *t2* (lt/to-local-date-time (t/date-time 2000 1 1 1 2)))

(defn conj-dt-fn [dt opts]
  (let [r (:atom opts)]
    (swap! r conj dt)))

(def ^:dynamic *cnj* (cj/cronj
            :entries [{:id       :conj
                       :handler  conj-dt-fn
                       :schedule "* * * * * * *"
                       :opts     {:atom *holder*}
                       :enabled  true}]))


(fact
  (do "Reset Store"
      (reset! *holder* []))

  (cj/disable-task *cnj* :conj)
  (ts/signal-tick (:scheduler *cnj*) :conj *t1*)
  (Thread/sleep 10)
  (count @*holder*) => 0

  (cj/enable-task *cnj* :conj)
  (ts/signal-tick (:scheduler *cnj*) :conj *t1*)
  
  (Thread/sleep 10)
  (count @*holder*) => 1
  (first @*holder*) => *t1*)

(fact
  (do "Simulate using single threaded execution"
      (reset! *holder* [])
      (time (sm/simulate-st *cnj* *t1* *t2* (t/seconds 1))))
  
  (Thread/sleep 10)
  (count @*holder*) => 61
  (first @*holder*) => *t1*
  (last @*holder*)  => *t2*)

(fact
  (do "Simulate using single threaded execution with task disabled"
      (reset! *holder* [])
      (cj/disable-task *cnj* :conj)
      (time (sm/simulate-st *cnj* *t1* *t2* (t/seconds 1))))
  
  (Thread/sleep 10)
  (count @*holder*) => 0
  (first @*holder*) => nil
  (last @*holder*)  => nil

  ;; Cleanup
  (cj/enable-task *cnj* :conj))

(fact
  (do "Simulate using multi-threaded execution with a pause of 1"
      (reset! *holder* [])
      (cj/shutdown! *cnj*)
      (time (sm/simulate *cnj* *t1* *t2* (t/seconds 2))))
  
  (Thread/sleep 1000)
  (count @*holder*) => 31
  (first @*holder*) => *t1*
  (last @*holder*)  => *t2*)

(fact
  (do "Simulate using multi-threaded execution with a pause of 1"
      (reset! *holder* [])
      (cj/shutdown! *cnj*)
      (time (sm/simulate *cnj* *t1* *t2* (t/seconds 2) 1)))
  
  (Thread/sleep 10)
  (count @*holder*) => 31
  (first @*holder*) => *t1*
  (last @*holder*)  => *t2*)


;; Additional code for mocking

(comment
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
                        (t/seconds 1)))

  (time (sm/simulate cnj
                     (lt/to-local-date-time (t/date-time 2000 1 1 1 1))
                     (lt/to-local-date-time (t/date-time 2000 1 1 1 2))
                     (t/seconds 1)))


)

;;(exec-st (first (:scheduler cnj)) (t/date-time 2000 1 1 1))

;;(ts/signal-tick (:scheduler cnj) (lt/to-local-date-time (t/date-time 2000 1 1 1 1)))

;;(:handler (:task (first (:scheduler cnj))))
