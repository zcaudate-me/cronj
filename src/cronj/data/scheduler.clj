(ns cronj.data.scheduler
  (:require [hara.state :refer [latch]]
            [hara.common.error :refer [suppress]]
            [hara.common.fn :refer [F]]
            [ova.core :as v]
            [cronj.data.tab :as tab]
            [cronj.data.task :as tk]))

(defn scheduler [] (v/ova))

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

(defn unschedule-task [tsc task-id]
  (dosync
   (doseq [entry (v/select tsc [[:task :id] task-id])]
    (tk/kill-all! (:task entry)))
   (v/remove! tsc [[:task :id] task-id])))

(defn reschedule-task [tsc task-id schedule]
  (dosync
   (v/!> tsc [[:task :id task-id]]
         (merge
          {:schedule  schedule
           :tab-array (set-tab-array-fn schedule)}))))

(defn schedule-task
  ([tsc tsce]
     (dosync
      (unschedule-task tsc (:id (:task tsce)))
      (v/insert! tsc tsce)))
  ([tsc task schedule & [enabled? opts]]
     (schedule-task tsc (task-entry task schedule enabled? opts))))

(defn enable-task [tsc task-id]
  (dosync
   (v/!> tsc [[:task :id] task-id]
         (merge {:enabled true}))))

(defn disable-task [tsc task-id]
  (dosync
   (v/!> tsc [[:task :id] task-id]
           (merge{:enabled false}))))

(defn task-enabled? [tsc task-id]
  (:enabled (first (v/select tsc [[:task :id] task-id]))))

(defn task-disabled? [tsc task-id]
  (not (task-enabled? tsc task-id)))

(defn- set-exec-output [dt entry]
  (reset! (:output entry)
          {:id (-> entry :task :id)
           :dt dt
           :exec (tk/exec! (:task entry)
                           (tab/truncate-ms dt) (:opts entry))}))

(defn- select-tasks [tsc task-id]
  (if task-id
    (v/select tsc [[:task :id] task-id :enabled true])
    (v/select tsc [:enabled true])))

(defn signal-tick
  ([tsc dt] (signal-tick tsc nil dt))
  ([tsc task-id dt]
     (doseq [entry (select-tasks tsc task-id)]
       (when (tab/match-array? (tab/to-dt-array dt) (:tab-array entry))
         (set-exec-output dt entry)))))

(defn trigger-tick
  ([tsc dt] [trigger-tick tsc nil dt])
  ([tsc task-id dt]
     (doseq [entry (select-tasks tsc task-id)]
       (set-exec-output dt entry))))

(defn get-task [tsc task-id]
  (first (v/select tsc [[:task :id] task-id])))

(defn task-ids [tsc]
  (map #(:id (:task %)) (v/selectv tsc)))

(defn task-threads [tsc]
  (map (fn [e]
         (if-let [task (:task e)]
           {:id (:id task)
            :running (tk/running task)}))
       (v/selectv tsc)))
