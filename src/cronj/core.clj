(ns cronj.core
  (:require [ova.core :as v]
            [hara.state :refer [add-change-watch]]
            [hara.import :as im]
            [cronj.data.task :as tk]
            [cronj.data.timer :as tm]
            [cronj.data.scheduler :as ts]))

(im/import cronj.simulation [simulate simulate-st local-time])
(im/import clj-time.local [local-now])

(declare install-watch cronj)

(defn cronj [& args]
  (let [scheduler (ts/scheduler)
        timer     (tm/timer)
        margs     (apply hash-map args)
        interval  (:interval margs)
        entries   (:entries  margs)]
    (if interval (swap! timer assoc :interval interval))
    (doseq [tsce (map ts/task-entry entries)]
      (ts/schedule-task scheduler tsce))
    (install-watch timer scheduler)
    {:timer timer :scheduler scheduler}))

(defn- install-watch [timer tsc]
  (add-change-watch
   timer :time-watch :last-check
   (fn [_ rf _ _]
     (let [r @rf]
       (ts/signal-tick tsc (:last-check-time r))))))


;;--------- timer functions --------------

(defn start! [cnj] (tm/start! (:timer cnj)))

(defn stop! [cnj] (tm/stop! (:timer cnj)))

(defn stopped? [cnj] (tm/stopped? (:timer cnj)))

(defn running? [cnj] (tm/running? (:timer cnj)))

(defn uptime [cnj] (tm/uptime (:timer cnj)))

;;--------- scheduler functions -----------

(defn schedule-task [cnj task schedule & [enabled? opts]]
  (ts/schedule-task (:scheduler cnj) task schedule enabled? opts))

(defn unschedule-task [cnj task-id]
  (ts/unschedule-task (:scheduler cnj) task-id))

(defn reschedule-task [cnj task-id schedule]
  (ts/reschedule-task (:scheduler cnj) task-id schedule))

(defn empty-tasks [cnj]
  (doseq [id (ts/task-ids (:scheduler cnj))]
    (ts/unschedule-task (:scheduler cnj) id)))

(defn enable-task [cnj task-id]
  (ts/enable-task (:scheduler cnj) task-id))

(defn disable-task [cnj task-id]
  (ts/disable-task (:scheduler cnj) task-id))

(defn task-enabled? [cnj task-id]
  (ts/task-enabled? (:scheduler cnj) task-id))

(defn task-disabled? [cnj task-id]
  (ts/task-disabled? (:scheduler cnj) task-id))

;;---------- task related functions -------

(defn get-ids [cnj]
  (ts/task-ids (:scheduler cnj)))

(defn get-task [cnj task-id]
  (ts/get-task (:scheduler cnj) task-id))

(defn get-threads
  ([cnj]
     (ts/task-threads (:scheduler cnj)))
  ([cnj task-id]
     (tk/running (-> (get-task cnj task-id) :task))))

(defn exec!
  ([cnj task-id]
     (exec! cnj task-id (local-now)))
  ([cnj task-id dt]
     (let [opts (-> (get-task cnj task-id) :opts)]
       (exec! cnj task-id dt opts)))
  ([cnj task-id dt opts]
     (tk/exec! (-> (get-task cnj task-id) :task) dt opts)))

(defn kill!
  ([cnj]
    (doseq [id (get-ids cnj)]
      (tk/kill-all! (-> (get-task cnj id) :task))))
  ([cnj task-id]
     (tk/kill-all! (-> (get-task cnj task-id) :task)))
  ([cnj task-id tid]
     (tk/kill! (-> (get-task cnj task-id) :task) tid)))

;;--------- system level ----------

(defn shutdown! [cnj]
  (stop! cnj)
  (kill! cnj))

(defn restart! [cnj]
  (shutdown! cnj)
  (start! cnj))
