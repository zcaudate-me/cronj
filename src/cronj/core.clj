(ns cronj.core
  (:require [hara.ova :as v]
            [hara.fn :as f]
            [cronj.data.task :as tk]
            [cronj.data.timer :as tm]
            [cronj.data.timetable :as tt]))

(declare install-watch)

(defn cronj [& args]
  (let [timetable (tt/timetable)
        timer     (tm/timer)
        margs     (apply hash-map args)
        interval  (:interval margs)
        entries   (:entries  margs)]
    ;;(println args entries)
    (if interval
      (swap! timer assoc :interval interval))
    (doseq [tte (map tt/tt-entry entries)]
      (tt/schedule-task timetable tte))
    (install-watch timer timetable)
    {:timer timer :timetable timetable}))

(defmacro defcronj [name & args]
  `(def ~name (cronj ~@args)))

(defn- install-watch [timer tt]
  (add-watch timer :time-watch
   (f/watch-for-change
    [:last-check]
    (fn [_ rf _ _]
      (let [r @rf]
        (tt/trigger-time tt (:last-check-time r)))))))


;;--------- timer functions --------------

(defn start! [cnj] (tm/start! (:timer cnj)))

(defn stop! [cnj] (tm/stop! (:timer cnj)))

(defn stopped? [cnj] (tm/stopped? (:timer cnj)))

(defn running? [cnj] (tm/running? (:timer cnj)))

(defn uptime [cnj] (tm/uptime (:timer cnj)))

;;--------- timetable functions -----------

(defn schedule-task [cnj task schedule & [enabled? opts]]
  (tt/schedule-task (:timetable cnj) task schedule enabled? opts))

(defn unschedule-task [cnj task-id]
  (tt/unschedule-task (:timetable cnj) task-id))

(defn empty-tasks [cnj]
  (doseq [id (tt/task-ids (:timetable cnj))]
    (tt/unschedule-task (:timetable cnj) id)))

(defn all-task-ids [cnj]
  (tt/task-ids (:timetable cnj)))

(defn all-threads [cnj]
  (tt/task-threads (:timetable cnj)))

(defn enable-task [cnj task-id]
  (tt/enable-task (:timetable cnj) task-id))

(defn disable-task [cnj task-id]
  (tt/disable-task (:timetable cnj) task-id))

(defn task-enabled? [cnj task-id]
  (tt/task-enabled? (:timetable cnj) task-id))

(defn task-disabled? [cnj task-id]
  (tt/task-disabled? (:timetable cnj) task-id))

;;---------- task related functions -------

(defn get-task [cnj task-id]
  (:task (tt/get-task (:timetable cnj) task-id)))

(defn task-threads
  ([cnj task-id]
     (tk/running (get-task cnj task-id)))
  ([cnj]
     ()))

(defn kill-task-thread [cnj task-id tid]
  (tk/kill! (get-task cnj task-id) tid))

(defn kill-threads
  ([cnj task-id]
     (tk/kill-all! (get-task cnj task-id)))
  ([cnj]
     (doseq [id (all-task-ids cnj)]
       (kill-threads cnj id))))

;;--------- system level ----------

(defn shutdown!! [cnj]
  (stop! cnj)
  (kill-threads cnj))

(defn restart!! [cnj]
  (shutdown!! cnj)
  (start! cnj))
