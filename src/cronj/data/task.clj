(ns cronj.data.task
  (require [hara.ova :as v]))

(def REQUIRED-TASK-KEYS [:id :handler])
(def ALL-TASK-KEYS [:id :desc :handler :running :last-exec :last-successful :pre-hook :post-hook])

(defn- has-tid? [ova id]
  (not (empty? (v/select ova [:tid id]))))

(defn task
  ([m]
     (into
      {:desc "" :running (v/ova) :last-exec (ref nil) :last-successful (ref nil)}
      m))

  ([id handler & opts]
      (->> {:id id :handler handler}
           (into
            (apply hash-map opts))
           (into
            {:desc "" :running (v/ova) :last-exec (ref nil) :last-successful (ref nil)}))))

(defn last-exec [task]
  @(:last-exec task))

(defn last-successful [task]
  @(:last-successful task))

(defn running [task]
  (->> (v/select (:running task))
       (map #(select-keys % [:tid :opts]))))

(defn- register-thread [task tid thd opts]
    (if (not (has-tid? (:running task) tid))
      (dosync (v/insert (:running task) {:tid tid :thread thd :opts opts})
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
     (if (has-tid? (:running task) tid)
       (dosync   (v/delete (:running task) [:tid tid])
                 (if success?
                   (ref-set (:last-successful task) tid)))
         (if (> (- current start) timeout)
           (throw (Exception. "Thread id deregistration timedout. tid not registered"))
           (recur task tid success? start (System/nanoTime) timeout)))))

(defn- exec-hook [hook tid opts]
  (if (fn? hook) (hook tid opts) opts))

(defn- exec-fn [task tid handler opts]
  ;;(println "exec-fn" opts)
  (try
    (let [post   (:post-hook task)
          result (handler tid opts)]
      (exec-hook post tid (assoc opts :result result))
      (deregister-thread task tid true))
    (catch Exception e)))

(defn exec! [task tid & [opts]]
  ;;(println "exec!" opts)
  (cond
    (has-tid? (:running task) tid) (println "There is already a thread with tid: " tid "running.")
    (not (or (nil? opts)
             (instance? clojure.lang.IPersistentMap opts))) (throw (Exception. (str "The opts argument has to be a hashmap, not " opts)))
    :else
    (let [pre      (:pre-hook task)
          handler  (:handler task)
          opts     (or opts {})
          fopts    (exec-hook pre tid opts)]
      (register-thread task tid (future (exec-fn task tid handler fopts)) fopts))))

(defn kill! [task tid]
  (let [thrds (v/select (:running task) [:tid tid])]
    (if-let [thrd (first thrds)]
      (do (future-cancel (:thread thrd))
          (deregister-thread task tid false))
      (println "Thread" tid "not running"))))

(defn kill-all! [task]
  (dosync
   (doseq [tid (map :tid (v/select (:running task)))]
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
  (->
   (select-keys task [:id :desc :last-exec :last-successful])
   (assoc :running (running task)
          :last-exec @(:last-exec task)
          :last-successful @(:last-successful task))))
