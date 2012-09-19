(ns cronj.core
  (:require [hara.data.dyna :as d]
            [cronj.timekeeper :as k]
            [cronj.timesheet :as ts] :reload))

(def ^:dynamic *cronj* (cronj.timekeeper/new))

(defn unschedule-all-tasks! [& [cj]] (ts/unschedule-all! (:timesheet @(or cj *cronj*))))
(defn schedule-task!
  ([task] (ts/schedule! (:timesheet @*cronj*) task))
  ([task tab & [cj]] (ts/schedule! (:timesheet @(or cj *cronj*)) task tab)))
(defn reschedule-task! [id tab & [cj]] (ts/reschedule! (:timesheet @(or cj *cronj*)) id tab))
(defn unschedule-task! [id & [cj]] (ts/unschedule! (:timesheet @(or cj *cronj*)) id))
(defn load-tasks! [tasks & [cj]] (ts/load! (:timesheet @(or cj *cronj*)) tasks))

(defn list-all-tasks [& [cj]] (ts/<all (:timesheet @(or cj *cronj*))))
(defn contains-task? [id & [cj]] (d/has-id? (:timesheet @(or cj *cronj*)) id))
(defn select-task [id & [cj]] (ts/select-task (:timesheet @(or cj *cronj*)) id))
(defn enable-task! [id & [cj]] (ts/enable-task! (:timesheet @(or cj *cronj*)) id))
(defn disable-task! [id & [cj]] (ts/disable-task! (:timesheet @(or cj *cronj*)) id))
(defn trigger-task! [id & [cj]] (ts/enable-task! (:timesheet @(or cj *cronj*)) id))
(defn list-running-for-task [id & [cj]] (ts/list-running (:timesheet @(or cj *cronj*)) id))
(defn kill-all-running-for-task! [id & [cj]] (ts/kill-all-running! (:timesheet @(or cj *cronj*)) id))
(defn kill-running-for-task! [id tid & [cj]] (ts/kill-running! (:timesheet @(or cj *cronj*)) id tid))
(defn last-exec-for-task [id & [cj]] (ts/last-exec (:timesheet @(or cj *cronj*)) id))
(defn last-successful-for-task [id & [cj]] (ts/last-successful (:timesheet @(or cj *cronj*)) id))

(defn stopped? [& [cj]] (k/stopped? (or cj *cronj*)))
(defn running? [& [cj]] (k/running? (or cj *cronj*)))
(defn start! [& [cj]] (k/start! (or cj *cronj*)))
(defn stop! [& [cj]] (k/stop! (or cj *cronj*)))
(defn restart! [& [cj]] (k/restart! (or cj *cronj*)))

(defn set-cronj!! [cj] (swap! *cronj* (fn [_] @cj)))
(defn new-cronj!! []
  (let [ret (cronj.timekeeper/new)]
        (unschedule-all-tasks! *cronj*)
        (stop! *cronj*)
    (set-cronj!! ret)
    ret))
