(ns cronj.core
  (:require [hara.ova :as v]
            [hara.fn :as f]
            [cronj.data.task :as tk]
            [cronj.data.timer :as tm]
            [cronj.data.timetable :as tt]))

(declare install-watch cronj-init)

(defn cronj
  ([]
     (let [timetable (tt/timetable)
           timer     (tm/timer)]
       (install-watch timer timetable)
       {:timer timer
        :timetable timetable}))
  ([& args]
     (let [cnj      (cronj)
           margs    (apply hash-map args)
           interval (:interval margs)
           entries  (:entries    margs)]
       (if interval
         (swap! (:timer cnj) assoc :interval interval))
       (cronj-init cnj entries)
       cnj)))

(defmacro defcronj [name & args]
  `(def ~name (cronj ~@args)))

(defn cronj-init [cnj entries]
  (let [tk-fn    (fn [e] (tk/task (select-keys e [:id :handler :desc])))
        tks      (map tk-fn entries)
        tt-fn    (fn [e] (select-keys e [:enabled :opts :schedule]))
        tts      (map tt-fn entries)]
    (dorun
     (map (fn [tk tt] (tt/schedule-task (:timetable cnj) tk tt)) tks tts))))

(defn- install-watch [timer tt]
  (add-watch timer :time-watch
   (f/watch-for-change
    [:last-check]
    (fn [_ rf _ _]
      (let [r @rf]
        (tt/trigger! tt (:last-check-time r)))))))

;;----------------------
(defn schedule-task [cnj task schedule & [enabled? opts]]
  (tt/schedule-task (:timetable cnj) task schedule enabled? opts))

(defn unschedule-task [cnj task-id]
  (tt/unschedule-task (:timetable cnj) task-id))

(defn unschedule-all [cnj]
  (doseq [id (tt/task-ids (:timetable cnj))]
    (tt/unschedule-task (:timetable cnj) id)))

(defn kill-threads [cnj task-id tid]
  (tk/kill! (tt/get-task (:timetable cnj) task-id) tid))

(defn kill-all-threads
  ([cnj task-id] (tk/kill-all! (tt/get-task (:timetable cnj))))
  ([cnj]
     (doseq [id (tt/task-ids (:timetable cnj))]
       (kill-all-threads cnj id))))

;;----------------------
(defn task-ids [cnj]
  (tt/task-ids (:timetable cnj)))

(defn task-threads [cnj]
  (tt/task-threads (:timetable cnj)))

(defn get-task [cnj task-id]
  (tt/get-task (:timetable cnj) task-id))

(defn enable-task [cnj task-id]
  (tt/enable-task (:timetable cnj) task-id))

(defn disable-task [cnj task-id]
  (tt/disable-task (:timetable cnj) task-id))

(defn task-enabled? [cnj task-id]
  (tt/task-enabled? (:timetable cnj) task-id))

(defn task-disabled? [cnj task-id]
  (tt/task-disabled? (:timetable cnj) task-id))

;;----------------------
(defn start! [cnj] (tm/start! (:timer cnj)))

(defn stop! [cnj] (tm/stop! (:timer cnj)))

(defn stopped? [cnj] (tm/stopped? (:timer cnj)))

(defn running? [cnj] (tm/running? (:timer cnj)))

(defn shutdown!! [cnj]
  (stop! cnj)
  (kill-all-threads cnj))

(defn restart!! [cnj]
  (shutdown!! cnj)
  (start! cnj))
