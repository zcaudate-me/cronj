(ns cronj.data.timetable
  (:use [hara.common :only [latch suppress]])
  (:require [hara.ova :as v]
            [cronj.data.tab :as tab]
            [cronj.data.task :as tk]))

(defn timetable []
  {:table (v/ova)
   :output (atom nil)})

(defn set-tab-array-fn [schedule]
  (suppress (tab/parse-tab schedule) tab/nil-array))

(defn task-entry
  ([m]
     (let [task (tk/task (select-keys m [:id :handler :desc :pre-hook :post-hook]))]
       (task-entry task (:schedule m) (:enabled m) (:opts m))))
  ([task schedule & [enabled? opts]]
     {:task      task
      :schedule  schedule
      :tab-array (set-tab-array-fn schedule)
      :enabled   (if (nil? enabled?) true
                     enabled?)
      :opts      (or opts {})}))

(defn unschedule-task [tt task-id]
  (dosync
   (doseq [entry (v/select (:table tt) [[:task :id] task-id])]
    (tk/kill-all! (:task entry)))
   (v/remove! (:table tt) [[:task :id] task-id])))

(defn reschedule-task [tt task-id schedule]
  (dosync
   (v/!>merge (:table tt) [[:task :id task-id]]
              {:schedule  schedule
               :tab-array (set-tab-array-fn schedule)})))

(defn schedule-task
  ([tt tte]
     (dosync
      (unschedule-task tt (:id (:task tte)))
      (v/insert! (:table tt) tte)))
  ([tt task schedule & [enabled? opts]]
     (schedule-task tt (task-entry task schedule enabled? opts))))

(defn enable-task [tt task-id]
  (dosync
   (v/!>merge (:table tt) [[:task :id] task-id] {:enabled true})))

(defn disable-task [tt task-id]
  (dosync
   (v/!>merge (:table tt) [[:task :id] task-id] {:enabled false})))

(defn task-enabled? [tt task-id]
  (:enabled (first (v/select (:table tt) [[:task :id] task-id]))))

(defn task-disabled? [tt task-id]
  (not (task-enabled? tt task-id)))

(defn- set-exec-output [tt dt entry]
  (reset! (:output tt)
               {:id (-> entry :task :id)
                :dt dt
                :exec (tk/exec! (:task entry)
                                (tab/truncate-ms dt) (:opts entry))}))

(defn- select-tasks [tt task-id]
  (if task-id
    (v/select (:table tt) [[:task :id] task-id :enabled true])
    (v/select (:table tt) [:enabled true])))

(defn signal-tick
  ([tt dt] (signal-tick tt nil dt))
  ([tt task-id dt]
     (doseq [entry (select-tasks tt task-id)]
       (when (tab/match-array? (tab/to-dt-array dt) (:tab-array entry))
         (set-exec-output tt dt entry)))))

(defn trigger-tick
  ([tt dt] [trigger-tick tt nil dt])
  ([tt task-id dt]
     (doseq [entry (select-tasks tt task-id)]
       (set-exec-output tt dt entry))))

(defn get-task [tt task-id]
  (first (v/select (:table tt) [[:task :id] task-id])))

(defn task-ids [tt]
  (map #(:id (:task %)) (v/<< (:table tt))))

(defn task-threads [tt]
  (map (fn [e]
         (if-let [task (:task e)]
           {:id (:id task)
            :running (tk/running task)}))
       (v/<< (:table tt))))
