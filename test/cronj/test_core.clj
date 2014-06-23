(ns cronj.test-core
  (:use midje.sweet
        hara.checkers)
  (:require [cronj.core :as cj]
            [cronj.data.scheduler :as ts]
            [cronj.data.task :as tk]
            [cronj.data.timer :as tm] :reload))

(defn has-length [counts]
  (fn [obj]
    (some #(= % (count obj)) counts)))

(def cnj (cj/cronj
          :interval 2
          :entries [{:id       :t1
                     :handler  (fn [dt opts] (println dt) (Thread/sleep 1000))
                     :schedule "* * * * * * *"
                     :enabled  true
                     :opts     {:home "/home/cronj"}}

                    {:id       :t2
                     :handler  (fn [dt opts] (println dt) (Thread/sleep 5000))
                     :schedule "* * * * * * *"
                     :enabled  true
                     :opts     {:ex "example"}}]))

(facts "Initialization of cronj"
  ;; scheduler queries
  (cj/get-ids cnj) => [:t1 :t2]
  (cj/get-threads cnj) => [{:id :t1 :running ()} {:id :t2 :running ()}]
  (cj/task-enabled? cnj :t1) => true
  (cj/task-disabled? cnj :t2) => false

  ;; timer queries
  (cj/running? cnj) => false
  (cj/stopped? cnj) => true
  (cj/uptime cnj) => nil

  ;; task queries
  (cj/get-threads cnj :t1) => ()
  (cj/get-threads cnj :t2) => ()

  ;; watch is installed
  (.getWatches (:timer cnj)) => #(contains? % :time-watch)
  )


(facts "Enabling and disabling tasks"
  (cj/task-enabled? cnj :t1) => true

  (do "disable task"
      (cj/disable-task cnj :t1)
      (cj/task-enabled? cnj :t1))
  => false

  (do "enable task"
      (cj/enable-task cnj :t1)
      (cj/task-enabled? cnj :t1))
  => true)

(facts "Scheduling and unscheduling new tasks"
  (let [cnj (cj/cronj)]

    (do "start up"
        (cj/start! cnj))

    (cj/get-threads cnj :t-temp) => nil?
    (cj/running? cnj) => true

    (do "add a task"
        (cj/schedule-task cnj (tk/task
                                {:id :t-temp
                                 :handler (fn [dt opts] (Thread/sleep 2000))})
                          "* * * * * * *"))
    (Thread/sleep 1000)
    (cj/get-threads cnj :t-temp) => (has-length #{1})
    (cj/get-ids cnj) => [:t-temp]
    (cj/task-enabled? cnj :t-temp) => true
    (cj/running? cnj) => true

    (do "remove a task"
        (cj/unschedule-task cnj :t-temp))
    (cj/get-ids cnj) => []
    (cj/running? cnj) => true

    (do "clean up"
        (cj/shutdown! cnj))))


(facts "Starting, stopping and killing"
  (let [cnj (cj/cronj
             :interval 2
             :entries [{:id       :t1
                        :handler  (fn [dt opts] (Thread/sleep 1000))
                        :schedule "* * * * * * *"
                        :enabled  true
                        :opts     {:home "/home/cronj"}}

                       {:id       :t2
                        :handler  (fn [dt opts] (Thread/sleep 5000))
                        :schedule "* * * * * * *"
                        :enabled  true
                        :opts     {:ex "example"}}])]
    (do "testing running"
        (cj/disable-task cnj :t1)
        (cj/start! cnj)
        (Thread/sleep 1000))

    (cj/get-threads cnj :t1) => ()
    (cj/get-threads cnj :t2) => (has-length #{1 2})

    (do "sleep 3 secs"
        (Thread/sleep 3000))

    (cj/get-threads cnj :t1) => ()
    (cj/get-threads cnj :t2) => (has-length #{4 5})

    (do "kill threads"
        (cj/kill! cnj :t2))

    (cj/get-threads cnj :t2) => (has-length #{0 1})

    (do "clean up"
        (cj/shutdown! cnj))

    (cj/get-ids cnj) => [:t1 :t2]

    (do "empty cronj"
        (cj/empty-tasks cnj))

    (cj/get-ids cnj) => []))
