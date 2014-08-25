(ns cronj.test-scheduler
  (:use midje.sweet
        ;;hara.checkers
        )
    (:require [hara.ova :as ova]
              [clj-time.local :as lt]
              [clj-time.core :as t]
              [cronj.data.task :as tk]
              [cronj.data.scheduler :as ts] :reload))

(facts "timesheet scheduling"
    (let [tscb (ts/scheduler)
          tk1 (tk/task :1 (fn [& _]))
          tk2 (tk/task :2 (fn [& _]))]

      (count tscb) => 0

      (ts/schedule-task tscb tk1 "* * * * * * *")
      (fact "should have 1 task" (count tscb) => 1)

      (ts/schedule-task tscb tk1 "* * * * * * *")
      (fact "should still have 1 task" (count tscb) => 1)

      (ts/schedule-task tscb tk2 "* * * * * * *")
      (fact "should have 2 tasks"
        (count tscb) => 2
        (ts/task-ids tscb) => (just [:1 :2] :in-any-order)
        (ts/task-threads tscb) => (just [{:id :1 :running []}
                                        {:id :2 :running []}]
                                       :in-any-order))

      (ts/schedule-task tscb tk2 "* * * * * * *")
      (fact "should still have 2 tasks" (count tscb) => 2)

      (ts/unschedule-task tscb :2)
      (fact "should still have 1 task" (count tscb) => 1)

      (ts/unschedule-task tscb :1)
      (fact "should still have no tasks" (count tscb) => 0)))

(facts "timesheet triggering"
  (let [out (atom nil)
        tscb (ts/scheduler)
        tk1 (tk/task :1 (fn [& _] (reset! out :1)))
        tk2 (tk/task :2 (fn [& _] (reset! out :2)))
        dt1 (t/from-time-zone (t/date-time 2002 1 1 1 1 1) (t/default-time-zone))
        dt2 (t/from-time-zone (t/date-time 2002 1 1 1 1 2) (t/default-time-zone))
        _   (ts/schedule-task tscb tk1 "1-60/2 * * * * * *")
        _   (ts/schedule-task tscb tk2 "2-60/2 * * * * * *")]

    (fact "initialization"
      @out => nil
      (count tscb) => 2
      (ts/task-enabled? tscb :1) => true
      (ts/task-enabled? tscb :2) => true)

    (ts/signal-tick tscb dt1)
    (let [[reg1 job1] (-> (tscb [:task :id] :1) :output deref :exec)]
      @job1 "out should be :1"
      @out => :1)
    (ts/signal-tick tscb dt2)
    (let [[reg2 job2] (-> (tscb [:task :id] :2) :output deref :exec)]
      @job2 "out should be :2"
      @out => :2)

    (ts/disable-task tscb :1)
    (ts/signal-tick tscb dt1)
    (let [[reg1 job1] (-> (tscb [:task :id] :1) :output deref :exec)]
      @job1 "out should be :2 as tk1 is disabled"
      @out => :2
      (ts/task-enabled? tscb :1) => false
      (ts/task-enabled? tscb :2) => true)

    (ts/enable-task tscb :1)
    (ts/signal-tick tscb dt1)
    (let [[reg1 job1] (-> (tscb [:task :id] :1) :output deref :exec)]
      @job1 "out should be :1 as tk1 is enabled"
      @out => :1
      (ts/task-enabled? tscb :1) => true
      (ts/task-enabled? tscb :2) => true)
))


(facts "longer running task"
  (let [tscb (ts/scheduler)
        tk1 (tk/task :1 (fn [& _] (Thread/sleep 100)))
        dt1 (t/from-time-zone (t/date-time 2002 1 1 1 1 1) (t/default-time-zone))
        dt2 (t/from-time-zone (t/date-time 2002 1 1 1 1 2) (t/default-time-zone))
        _   (ts/schedule-task tscb tk1 "* * * * * * *")]
    (fact "should have 1 task, no threads running"
      (count tscb) => 1
      (ts/task-threads tscb) => [{:id :1 :running []}])

    (ts/signal-tick tscb dt1)
    (Thread/sleep 10)
    (fact "one thread should be running"
      (ts/task-threads tscb) => [{:id :1 :running [{:tid dt1 :opts {}}]}])

    (ts/signal-tick tscb dt2) (Thread/sleep 10)
    (fact "two threads should be running"
      (ts/task-threads tscb) => [{:id :1 :running [{:tid dt1 :opts {}} {:tid dt2 :opts {}}]}])

    (Thread/sleep 100)
    (fact "no threads running"
      (ts/task-threads tscb) => [{:id :1 :running []}])))
