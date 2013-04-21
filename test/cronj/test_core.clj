(ns cronj.test-core
  (:use midje.sweet
        hara.checkers)
  (:require [cronj.core :as cj]
            [cronj.data.timetable :as tt]
            [cronj.data.task :as tk]
            [cronj.data.timer :as tm] :reload))

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
  ;; timetable queries
  (cj/all-task-ids cnj) => [:t1 :t2]
  (cj/all-threads cnj) => [{:id :t1 :running ()} {:id :t2 :running ()}]
  (cj/task-enabled? cnj :t1) => true
  (cj/task-disabled? cnj :t2) => false

  ;; timer queries
  (cj/running? cnj) => false
  (cj/stopped? cnj) => true
  (cj/uptime cnj) => nil

  ;; task queries
  (cj/task-threads cnj :t1) => ()
  (cj/task-threads cnj :t2) => ()

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


(facts "Enabling and disabling tasks"
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

    (cj/task-threads cnj :t1) => ()
    (cj/task-threads cnj :t2) => nil

    (do "sleep 3 secs"
        (Thread/sleep 3000))

    (cj/task-threads cnj :t1) => ()
    (cj/task-threads cnj :t2) => nil

    (do "kill threads"
        (cj/kill-threads cnj :t2))

    (cj/task-threads cnj :t2) => nil

    (do "clean up"
        (cj/shutdown!! cnj))

    (cj/all-task-ids cnj) => [:t1 :t2]

    (do "empty cronj"
        (cj/empty-tasks cnj))

    (cj/all-task-ids cnj) => []))
