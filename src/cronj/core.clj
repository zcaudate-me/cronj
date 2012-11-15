(ns cronj.core
  (:require [hara.dyna :as d]
            [hara.fn :as f]
            [cronj.loop :as lp]
            [cronj.global :as g]
            [cronj.data.timesheet :as ts]))


(defn unschedule-all-tasks! [] (ts/unschedule-all! g/*timesheet*))

(defn schedule-task!
  ([task] (ts/schedule! g/*timesheet* task))
  ([task tab] (ts/schedule! g/*timesheet* task tab)))

(defn reschedule-task! [id tab] (ts/reschedule! g/*timesheet* id tab))

(defn unschedule-task! [id] (ts/unschedule! g/*timesheet* id))

(defn load-tasks! [tasks] (ts/load! g/*timesheet* tasks))

(defn list-all-tasks [] (ts/<all g/*timesheet*))

(defn list-all-task-ids [] (d/ids g/*timesheet*))

(defn contains-task? [id] (d/has-id? g/*timesheet* id))

(defn select-task [id] (ts/select-task g/*timesheet* id))

(defn enable-task! [id] (ts/enable-task! g/*timesheet* id))

(defn disable-task! [id] (ts/disable-task! g/*timesheet* id))

(defn trigger-task! [id] (ts/trigger-task! g/*timesheet* id))

(defn list-running-for-task [id] (ts/list-running g/*timesheet* id))

(defn kill-all-running-for-task! [id] (ts/kill-all-running! g/*timesheet* id))

(defn kill-running-for-task! [id tid] (ts/kill-running! g/*timesheet* id tid))

(defn last-exec-for-task [id] (ts/last-exec g/*timesheet* id))

(defn last-successful-for-task [id] (ts/last-successful g/*timesheet* id))

(defn stopped? [] (lp/stopped? g/*timeloop*))

(defn running? [] (lp/running? g/*timeloop*))

(defn start! [] (lp/start! g/*timeloop*))

(defn stop! [] (lp/stop! g/*timeloop*))

(defn restart! []
  (stop!)
  (start!))

(defn shutdown!! []
  (stop!)
  (doseq [id (list-all-task-ids)]
    (kill-all-running-for-task! id))
  (unschedule-all-tasks!))
