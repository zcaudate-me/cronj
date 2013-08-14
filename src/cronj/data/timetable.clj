(ns cronj.data.timetable
  (:require [hara.state :refer [latch]]
            [hara.common.error :refer [suppress]]
            [hara.common.fn :refer [F]]
            [ova.core :as v]
            [cronj.data.tab :as tab]
            [cronj.data.task :as tk]))

(defn timetable [] (v/ova))

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
      :opts      (or opts {})
      :output    (atom nil)}))

(defn unschedule-task [tt task-id]
  (dosync
   (doseq [entry (v/select tt [[:task :id] task-id])]
    (tk/kill-all! (:task entry)))
   (v/remove! tt [[:task :id] task-id])))

(defn reschedule-task [tt task-id schedule]
  (dosync
   (v/!> tt [[:task :id task-id]]
         merge
         {:schedule  schedule
          :tab-array (set-tab-array-fn schedule)})))

(defn schedule-task
  ([tt tte]
     (dosync
      (unschedule-task tt (:id (:task tte)))
      (v/insert! tt tte)))
  ([tt task schedule & [enabled? opts]]
     (schedule-task tt (task-entry task schedule enabled? opts))))

(defn enable-task [tt task-id]
  (dosync
   (v/!> tt [[:task :id] task-id]
         merge {:enabled true})))

(defn disable-task [tt task-id]
  (dosync
   (v/!> tt [[:task :id] task-id]
           merge{:enabled false})))

(defn task-enabled? [tt task-id]
  (:enabled (first (v/select tt [[:task :id] task-id]))))

(defn task-disabled? [tt task-id]
  (not (task-enabled? tt task-id)))

(defn- set-exec-output [dt entry]
  (reset! (:output entry)
          {:id (-> entry :task :id)
           :dt dt
           :exec (tk/exec! (:task entry)
                           (tab/truncate-ms dt) (:opts entry))}))

(defn- select-tasks [tt task-id]
  (if task-id
    (v/select tt [[:task :id] task-id :enabled true])
    (v/select tt [:enabled true])))

(defn signal-tick
  ([tt dt] (signal-tick tt nil dt))
  ([tt task-id dt]
     (doseq [entry (select-tasks tt task-id)]
       (when (tab/match-array? (tab/to-dt-array dt) (:tab-array entry))
         (set-exec-output dt entry)))))

(defn trigger-tick
  ([tt dt] [trigger-tick tt nil dt])
  ([tt task-id dt]
     (doseq [entry (select-tasks tt task-id)]
       (set-exec-output dt entry))))

(defn get-task [tt task-id]
  (first (v/select tt [[:task :id] task-id])))

(defn task-ids [tt]
  (map #(:id (:task %)) (v/<< tt)))

(defn task-threads [tt]
  (map (fn [e]
         (if-let [task (:task e)]
           {:id (:id task)
            :running (tk/running task)}))
       (v/<< tt)))
