(ns cronj.task
  (require [hara.data.dyna :as d]))

(def REQUIRED-TASK-KEYS [:id :desc :handler])
(def ALL-TASK-KEYS [:id :desc :handler :enabled :args :running :last-called :last-successful])

(defn new
  ([m]
     (into
      {:enabled (atom true) :running (hara.data.dyna/new) :args {} :last-called (atom nil) :last-successful (atom nil)}
      m))

  ([id desc handler & opts]
      (->> {:id id :desc desc :handler handler}
           (into
            (apply hash-map opts))
           (into
            {:enabled (atom true) :running (hara.data.dyna/new) :args {} :last-called (atom nil) :last-successful (atom nil)}))))

(defn is-task? [m]
  (every? true? (map #(contains? m %) ALL-TASK-KEYS)))

(defn enable [task]
  {:pre [(is-task? task)]}
  (swap! (task :enabled) (constantly true))
  task)

(defn disable [task]
  {:pre [(is-task? task)]}
  (swap! (task :enabled) (constantly false))
  task)

(defn enabled? [task] @(:enabled task))

(def disabled? (comp not enabled?))

(defn- register-thread [task tid thd]
  (d/insert! (:running task) {:id tid :thread thd})
  (swap! (:last-called task) (fn [_] tid))
  task)

(defn- deregister-thread
  ([task tid] (deregister-thread task tid true))
  ([task tid success?]
     (Thread/sleep 100)
     (d/delete! (:running task) tid)
     (if success?
       (swap! (:last-successful task) (fn [_] tid)))
     task))

(defn- exec-fn [task tid handler args]
  (try
    (do
      (apply handler tid (flatten (vec args)))
      (deregister-thread task tid))
    (catch Exception e
      (println e)
      (deregister-thread task tid false))))

(defn exec! [task tid]
  {:pre [(is-task? task)]}
  (cond
    (disabled? task) (println "Task (" (:id task) ") is not enabled")

    (d/has-id? (:running task) tid) (println "There is already a thread with id: " tid "running.")

    :else
    (let [handler (:handler task)
          args    (:args task)]
      (register-thread task tid
       (future (exec-fn task tid handler args))))))

(defn last-called [task]
  {:pre [(is-task? task)]}
  @(:last-called task))

(defn last-successful [task]
  {:pre [(is-task? task)]}
  @(:last-successful task))

(defn running [task]
  {:pre [(is-task? task)]}
  (->> (d/search (:running task))
      (map :id)))

(defn kill! [task tid]
  {:pre [(is-task? task)]}
  (let [tcon (d/select (:running task) tid)]
    (future-cancel (:thread tcon))
    (deregister-thread task tid false)))

(defn kill-all! [task]
  (doseq [tid (d/ids (:running task))]
    (kill! task tid)))

(defn reinit! [task]
  (kill-all! task)
  (swap! (:last-called task)
         (fn [_] nil))
  (swap! (:last-successful task)
         (fn [_] nil))
  task)

(defn <# [task]
  {:pre [(is-task? task)]}
  (->
   (select-keys task [:id :desc :enabled :last-called :last-successful])
   (assoc :running (running task)
          :enabled @(:enabled task)
          :last-called @(:last-called task)
          :last-successful @(:last-successful task))))
