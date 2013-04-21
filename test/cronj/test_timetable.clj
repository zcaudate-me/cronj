(ns cronj.test-timetable
  (:use midje.sweet
        hara.checkers)
    (:require [hara.ova :as v]
              [clj-time.local :as lt]
              [clj-time.core :as t]
              [cronj.data.task :as tk]
              [cronj.data.timetable :as tt] :reload))

(facts "timesheet scheduling"
    (let [ttb (tt/timetable)
          tk1 (tk/task :1 (fn [& _]))
          tk2 (tk/task :2 (fn [& _]))]
      (fact "initialization"
        ttb => (just {:table (is-ova)
                      :output (is-atom nil)}))

      (count (:table ttb)) => 0

      (tt/schedule-task ttb tk1 "* * * * * * *")
      (fact "should have 1 task" (count (:table ttb)) => 1)

      (tt/schedule-task ttb tk1 "* * * * * * *")
      (fact "should still have 1 task" (count (:table ttb)) => 1)

      (tt/schedule-task ttb tk2 "* * * * * * *")
      (fact "should have 2 tasks"
        (count (:table ttb)) => 2
        (tt/task-ids ttb) => (just [:1 :2] :in-any-order)
        (tt/task-threads ttb) => (just [{:id :1 :running []}
                                        {:id :2 :running []}]
                                       :in-any-order))

      (tt/schedule-task ttb tk2 "* * * * * * *")
      (fact "should still have 2 tasks" (count (:table ttb)) => 2)

      (tt/unschedule-task ttb :2)
      (fact "should still have 1 task" (count (:table ttb)) => 1)

      (tt/unschedule-task ttb :1)
      (fact "should still have no tasks" (count (:table ttb)) => 0)))

(facts "timesheet triggering"
  (let [out (atom nil)
        ttb (tt/timetable)
        tk1 (tk/task :1 (fn [& _] (reset! out :1)))
        tk2 (tk/task :2 (fn [& _] (reset! out :2)))
        dt1 (t/from-time-zone (t/date-time 2002 1 1 1 1 1) (t/default-time-zone))
        dt2 (t/from-time-zone (t/date-time 2002 1 1 1 1 2) (t/default-time-zone))
        _   (tt/schedule-task ttb tk1 "1-60/2 * * * * * *")
        _   (tt/schedule-task ttb tk2 "2-60/2 * * * * * *")]

    (fact "initialization"
      out => (is-atom nil)
      (count (:table ttb)) => 2
      (tt/task-enabled? ttb :1) => true
      (tt/task-enabled? ttb :2) => true)

   (tt/signal-tick ttb dt1)
    (let [[reg1 job1] (-> @(:output ttb) :exec)]
      @job1 "out should be :1"
      out => (is-atom :1))
    (tt/signal-tick ttb dt2)
    (let [[reg2 job2] (-> @(:output ttb) :exec)]
      @job2 "out should be :2"
      out => (is-atom :2))

    (tt/disable-task ttb :1)
    (tt/signal-tick ttb dt1)
    (let [[reg1 job1] (-> @(:output ttb) :exec)]
      @job1 "out should be :2 as tk1 is disabled"
      out => (is-atom :2)
      (tt/task-enabled? ttb :1) => false
      (tt/task-enabled? ttb :2) => true)

    (tt/enable-task ttb :1)
    (tt/signal-tick ttb dt1)
    (let [[reg1 job1] (-> @(:output ttb) :exec)]
      @job1 "out should be :1 as tk1 is enabled"
      out => (is-atom :1)
      (tt/task-enabled? ttb :1) => true
      (tt/task-enabled? ttb :2) => true)
))

(facts "longer running task"
  (let [ttb (tt/timetable)
        tk1 (tk/task :1 (fn [& _] (Thread/sleep 100)))
        dt1 (t/from-time-zone (t/date-time 2002 1 1 1 1 1) (t/default-time-zone))
        dt2 (t/from-time-zone (t/date-time 2002 1 1 1 1 2) (t/default-time-zone))
        _   (tt/schedule-task ttb tk1 "* * * * * * *")]
    (fact "should have 1 task, no threads running"
      (count (:table ttb)) => 1
      (tt/task-threads ttb) => [{:id :1 :running []}])

    (tt/signal-tick ttb dt1)
    (Thread/sleep 10)
    (fact "one thread should be running"
      (tt/task-threads ttb) => [{:id :1 :running [{:tid dt1 :opts {}}]}])

    (tt/signal-tick ttb dt2) (Thread/sleep 10)
    (fact "two threads should be running"
      (tt/task-threads ttb) => [{:id :1 :running [{:tid dt1 :opts {}} {:tid dt2 :opts {}}]}])

    (Thread/sleep 100)
    (fact "no threads running"
      (tt/task-threads ttb) => [{:id :1 :running []}])))
