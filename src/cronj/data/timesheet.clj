(ns cronj.data.timesheet
  (:require [hara.dyna :as d]
            [cronj.data.tab :as tab]
            [cronj.data.task :as task]))

(defn timesheet [] (d/dyna))

(defn schedule!
  ([ts m]
    {:pre [(contains? m :tab)]}
    (schedule! ts m (:tab m)))
  ([ts m tab-str]
     (let [tk  (cond (task/is-task? m) m
                     :else (task/task m))
           ttk (tab/assoc-tab (dissoc tk :tab) tab-str)]
        (d/insert! ts ttk)))
  ([ts id desc handler tab-str & opts]
     (let [tk  (apply task/task id desc handler opts)
           ttk (tab/assoc-tab tk tab-str)]
       (d/insert! ts ttk))))

(defn reschedule! [ts id tab-str]
  {:pre [(d/has-id? ts id)]}
  (d/op! ts id tab/assoc-tab tab-str))

(defn unschedule! [ts id]
  (let [tk (d/select ts id)]
    (task/kill-all! tk)
    (d/delete! ts id)))

(defn unschedule-all! [ts]
  (let [ids (d/ids ts)]
    (map #(unschedule! ts %) ids)))

(defn load! [ts ms]
  (unschedule-all! ts)
  (doseq [m ms]
    (schedule! ts m)))

(defn- task-op! [ts id op & opts]
  (let [tk (d/select ts id)]
    (apply op tk opts)))

(defmacro deftaskop [name args op]
  {:pre [(vector? args)]}
  (list `defn (symbol name)  (apply vector 'ts 'id args)
        (apply list `task-op! 'ts 'id op args)))

(deftaskop select-task [] task/<#)
(deftaskop enable-task! [] task/enable)
(deftaskop disable-task! [] task/disable)
(deftaskop trigger-task! [dt] task/exec!)
(deftaskop list-running [] task/running)
(deftaskop kill-all-running! [] task/kill-all!)
(deftaskop kill-running! [tid] task/kill!)
(deftaskop last-exec [] task/last-exec)
(deftaskop last-successful [] task/last-successful)

(defn <all [ts]
  (let [tks (d/search ts)]
    (map #(apply merge (task/<# %) (select-keys % [:tab-str])) tks)))

(defn trigger-matched! [ts dt dt-arr]
  (doseq [task (d/search ts)]
    (if (tab/match-arr? dt-arr (:tab-arr task))
      (trigger-task! ts (:id task) (tab/truncate-ms dt)))))
