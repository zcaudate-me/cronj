(ns cronj.data.task
  (require [hara.dyna :as d]))

(def REQUIRED-TASK-KEYS [:id :desc :handler])
(def ALL-TASK-KEYS [:id :desc :handler :enabled :args :running :last-exec :last-successful])

(defn task
  ([m]
     (into
      {:enabled (atom true) :running (d/dyna) :args {} :last-exec (atom nil) :last-successful (atom nil)}
      m))

  ([id desc handler & opts]
      (->> {:id id :desc desc :handler handler}
           (into
            (apply hash-map opts))
           (into
            {:enabled (atom true) :running (d/dyna) :args {} :last-exec (atom nil) :last-successful (atom nil)}))))

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
  (swap! (:last-exec task) (fn [_] tid))
  task)

(defn- deregister-thread
  ([task tid] (deregister-thread task tid true))
  ([task tid success?]
     (Thread/sleep 1) ;; This call to sleep lasts 100ms - a bloody long time can we take it out?
     (d/delete! (:running task) tid)
    ;;(println "Deregistering thread" tid)
     (if success?
       (swap! (:last-successful task) (fn [_] tid)))
    ;;(println "Last successful:" (:last-successful task))
     task))

(defn- exec-hook [hook tid args]
  (if (fn? hook) (hook tid args) args))

(defn- exec-fn [task tid handler args]
  (try
    (let [post   (:post-hook task)
          arglst (mapcat identity (vec args))
          result (apply handler tid arglst)]
      (exec-hook post tid (assoc args :result result))
      (deregister-thread task tid true))
    (catch Exception e
      (println e)
      (deregister-thread task tid false))))

(defn exec! [task tid]
  ;;{:pre [(is-task? task)]}
  (cond
    (disabled? task) (println "Task (" (:id task) ") is not enabled")

    (d/has-id? (:running task) tid) (println "There is already a thread with id: " tid "running.")

    :else
    (let [pre      (:pre-hook task)
          handler  (:handler task)
          args     (:args task)]
      (register-thread
       task tid
       (future (exec-fn task tid handler (exec-hook pre tid args)))))))

(defn last-exec [task]
  ;;{:pre [(is-task? task)]}
  @(:last-exec task))

(defn last-successful [task]
  ;;{:pre [(is-task? task)]}
  @(:last-successful task))

(defn running [task]
  ;;{:pre [(is-task? task)]}
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
  (swap! (:last-exec task)
         (fn [_] nil))
  (swap! (:last-successful task)
         (fn [_] nil))
  task)

(defn <# [task]
  {:pre [(is-task? task)]}
  (->
   (select-keys task [:id :desc :enabled :last-exec :last-successful])
   (assoc :running (running task)
          :enabled @(:enabled task)
          :last-exec @(:last-exec task)
          :last-successful @(:last-successful task))))
