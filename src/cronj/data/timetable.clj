(ns cronj.data.timetable
  (:require [hara.ova :as v]
            [cronj.data.tab :as tab]
            [cronj.data.task :as tk]))

(defn timetable [] (v/ova))

(defn tt-entry
  ([m]
     (let [task (tk/task (select-keys m [:id :handler :desc :pre-hook :post-hook]))]
       (tt-entry task (:schedule m) (:enabled m) (:opts m))))
  ([task schedule & [enabled? opts]]
     {:task     task
      :schedule schedule
      :tab-arr  (tab/parse-tab schedule)
      :enabled  (if (nil? enabled?) true
                    enabled?)
      :opts     (or opts {})}))

(defn unschedule-task [tt task-id]
  (dosync
   (doseq [entry (v/select tt [[:task :id] task-id])]
    (tk/kill-all! (:task entry)))
   (v/delete tt [[:task :id] task-id])))

(defn schedule-task
  ([tt tte]
     ;;(println "add task:" tte)
     (dosync
      (unschedule-task tt (:id (:task tte)))
      (v/insert tt tte)))
  ([tt task schedule & [enabled? opts]]
     (schedule-task tt (tt-entry task schedule enabled? opts))))

(defn enable-task [tt task-id]
  (dosync
   (v/update tt [[:task :id] task-id] {:enabled true})))

(defn disable-task [tt task-id]
  (dosync
   (v/update tt [[:task :id] task-id] {:enabled false})))

(defn task-enabled? [tt task-id]
  (:enabled (first (v/select tt [[:task :id] task-id]))))

(defn task-disabled? [tt task-id]
  (not (task-enabled? tt task-id)))

(defn trigger-time [tt dt]
  (let [dt-arr (tab/to-dt-arr dt)]
    (doseq [entry (v/select tt [:enabled true])]
      (if (tab/match-arr? dt-arr (:tab-arr entry))
        (tk/exec! (:task entry) (tab/truncate-ms dt) (:opts entry))))))

(defn get-task [tt task-id]
  (first (v/select tt [[:task :id] task-id])))

(defn task-ids [tt]
  (map #(:id (:task %)) (v/select tt)))

(defn task-threads [tt]
  (map (fn [e]
         (if-let [task (:task e)]
           {:id (:id task)
            :running (tk/running task)}))
       (v/select tt)))
