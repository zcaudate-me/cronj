(ns cronj.data.task
  (:require [hara.ova :as ova]
            [hara.common.error :refer [error suppress]]
            [hara.common.checks :refer [hash-map?]]
            [clojure.tools.logging :as log]))

(def REQUIRED-TASK-KEYS [:id :handler])
(def ALL-TASK-KEYS [:id :desc :handler :running :last-exec :last-successful :pre-hook :post-hook])

(defn- has-tid? [ova id]
  (ova/has? ova [:tid id]))

(defn task
  ([m] (into {:desc "" :running (ova/ova) :last-exec (ref nil)
              :last-successful (ref nil)}
             m))

  ([id handler & opts]
      (->> {:id id :handler handler}
           (into (apply hash-map opts))
           (into {:desc "" :running (ova/ova) :last-exec (ref nil)
                  :last-successful (ref nil)}))))

(defn last-exec [task]
  @(:last-exec task))

(defn last-successful [task]
  @(:last-successful task))

(defn running [task]
  (->> (ova/selectv (:running task))
       (map #(select-keys % [:tid :opts]))))


(defn- register-thread [task tid threadp opts]
  (when (not (has-tid? (:running task) tid))
    (dosync (ova/insert! (:running task) {:tid tid :thread threadp :opts opts})
            (ref-set (:last-exec task) tid))
    task))

(defn- deregister-thread
  ([task tid] (deregister-thread task tid true))
  ([task tid finished?]
     (if (has-tid? (:running task) tid)
       (dosync   (ova/remove! (:running task) [:tid tid])
                 (if finished?
                   (ref-set (:last-successful task) tid))))
     task))

(defn- exec-hook [hook tid opts]
  (if (fn? hook) (hook tid opts) opts))

(defn- exec-fn [regp tid handler opts]
  (suppress
   (if-let [task  @regp]
     (let [post   (:post-hook task)
           result (handler tid opts)]
       (exec-hook post tid (assoc opts :result result))
       (deregister-thread task tid true))
     (log/info "Task registration failed for " tid))))

(defn exec-main [task tid opts]
  (let [pre       (:pre-hook task)
        handler   (:handler task)
        opts      (or opts {})
        fopts     (exec-hook pre tid opts)
        regp      (promise)
        threadp   (future (exec-fn regp tid handler fopts))]
      (deliver regp (register-thread task tid threadp fopts))
      [regp threadp]))

(defn exec! [task tid & [opts]]
  (cond (has-tid? (:running task) tid)
        (log/info "There is already a thread with tid: " tid "running.")

        (not (or (nil? opts) (hash-map? opts)))
        (error "The opts argument has to be a hashmap, not " opts)

        :else
        (exec-main task tid opts)))

(defn kill! [task tid]
  (let [thrds (ova/selectv (:running task) [:tid tid])]
    (if-let [thrd (first thrds)]
      (do (future-cancel (:thread thrd))
          (deregister-thread task tid false))
      (log/info "Thread" tid "not running"))))

(defn kill-all! [task]
  (dosync
   (doseq [tid (map :tid (ova/selectv (:running task)))]
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
