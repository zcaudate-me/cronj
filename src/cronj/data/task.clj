(ns cronj.data.task
  (require [hara.ova :as v]))

(def REQUIRED-TASK-KEYS [:id :handler])
(def ALL-TASK-KEYS [:id :desc :handler :enabled :args :running :last-exec :last-successful])

(defn- has-id? [ova id]
  (not (empty? (v/select ova [:id id]))))

(defn task
  ([m]
     (into
      { :desc "" :enabled (ref true) :running (v/ova) :args {} :last-exec (ref nil) :last-successful (ref nil)}
      m))

  ([id handler & opts]
      (->> {:id id :handler handler}
           (into
            (apply hash-map opts))
           (into
            {:desc "" :enabled (ref true) :running (v/ova) :args {} :last-exec (ref nil) :last-successful (ref nil)}))))

(defn is-task? [m]
  (every? true? (map #(contains? m %) ALL-TASK-KEYS)))

(defn enable [task]
  (dosync (alter (task :enabled) (constantly true)))
  task)

(defn disable [task]
  (dosync (alter (task :enabled) (constantly false)))
  task)

(defn enabled? [task] @(:enabled task))

(def disabled? (comp not enabled?))

(defn last-exec [task]
  @(:last-exec task))

(defn last-successful [task]
  @(:last-successful task))

(defn running [task]
  (->> (v/select (:running task))
       (map :id)))

(defn- register-thread [task tid thd]
    (if (not (has-id? (:running task) tid))
      (dosync (v/insert! (:running task) {:id tid :thread thd})
              (ref-set (:last-exec task) tid))
      (throw (Exception. "Thread id already exists")))
    task)

(defn- deregister-thread
  ([task tid] (deregister-thread task tid true))
  ([task tid success?]
     (let [start (System/nanoTime)]
       (deregister-thread task tid success? start start 100000))
     task)
  ([task tid success? start current timeout]
     (Thread/sleep 1) ;; Sleep to let the thread register itself
     (if (has-id? (:running task) tid)
       (dosync   (v/delete! (:running task) [:id tid])
                 (if success?
                   (ref-set (:last-successful task) tid)))
         (if (> (- current start) timeout)
           (throw (Exception. "Thread id deregistration timedout. tid not registered"))
           (recur task tid success? start (System/nanoTime) timeout)))))

(defn- exec-hook [hook tid args]
  (if (fn? hook) (hook tid args) args))

(defn- exec-fn [task tid handler args]
  (try
    (let [post   (:post-hook task)
          arglst (mapcat identity (vec args))
          result (apply handler tid arglst)]
      (exec-hook post tid (assoc args :result result))
      (deregister-thread task tid true))
    (catch Exception e)))

(defn exec! [task tid]
  (cond
    (disabled? task) (println "Task (" (:id task) ") is not enabled")

    (has-id? (:running task) tid) (println "There is already a thread with id: " tid "running.")

    :else
    (let [pre      (:pre-hook task)
          handler  (:handler task)
          args     (:args task)
          args     (exec-hook pre tid args)]
      (register-thread task tid (future (exec-fn task tid handler args))))))

(defn kill! [task tid]
  {:pre [(is-task? task)]}
  (let [thrds (v/select (:running task) [:id tid])]
    (if-let [thrd (first thrds)]
      (do (future-cancel (:thread thrd))
          (deregister-thread task tid false))
      (println "Thread" tid "not running"))))

(defn kill-all! [task]
  (dosync
   (doseq [tid (map :id (v/select (:running task)))]
     (kill! task tid))))


(defn reinit! [task]
  (dosync
   (kill-all! task)
   (alter (:last-exec task)
          (fn [_] nil))
   (alter (:last-successful task)
          (fn [_] nil)))
  task)

(defn <# [task]
  {:pre [(is-task? task)]}
  (->
   (select-keys task [:id :desc :enabled :last-exec :last-successful])
   (assoc :running (running task)
          :enabled @(:enabled task)
          :last-exec @(:last-exec task)
          :last-successful @(:last-successful task))))
