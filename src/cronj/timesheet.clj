(ns cronj.timesheet
  (:require [hara.data.dyna :as d]
            [cronj.tab :as tab]
            [cronj.task :as task] :reload))

(defn new []
  (hara.data.dyna/new))

(defn schedule!
  ([ts m]
    (println m)
    (schedule! ts (dissoc m :tab) (:tab m)))
  ([ts m tab-str]
     (let [tk  (task/new m)
           ttk (tab/assoc-tab tk tab-str)]
        (d/insert! ts ttk)))
  ([ts id desc handler tab-str & opts]
     (let [tk  (apply task/new id desc handler opts)
           ttk (tab/assoc-tab tk tab-str)]
       (d/insert! ts ttk))))

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

(defn <all [ts]
  (let [tks (d/search ts)]
    (map #(apply merge (task/<# %) (select-keys % [:tab-str])) tks)))

(defn trigger-matched! [ts dt dt-arr]
  (doseq [task (d/search ts)]
    (if (tab/match-arr? dt-arr (:tab-arr task))
      (trigger-task! ts (:id task) dt))))
