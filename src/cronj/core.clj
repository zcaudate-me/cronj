;;   Diagram
;;
;;                                        job-list
;;                __...--+----+----+----+----+----+----+----+----+
;;       _..---'""      _|.--"|    |    |    |    |    |    |    |
;;      +-------------+'_+----+----+----+----+----+----+----+----+
;;      | :id         |-     /                                 |
;;      | :desc       |     /     dpL                       job methods
;;      | :handler    |    /    ,XPXYb.               +---------------------+
;;      | :schedule   |   /    dP']X[`XL              | add-(all)-job(s)    |
;;      | :enabled    |  /    `'  ]X[  "              | remove-(all)-job(s) |
;;      |             | /         ]X[                 | enable(d?)          |
;;      |             |/          ]X[                 | disable(d?)         |
;;      +-------------+           ]X[                 | toggle              |
;;           job               ....:.....             | enabled-job(-id)s   |
;;                         ...!''''''''''!..          | disabled-job(-id)s  |
;;                       ,,'''            '`!.        | $job                |
;;                      .!'    every second  `!.      +---------------------+
;;            ................  looks at the   !.
;;            | status map   |  job-list and   `!.
;;            |              |  triggers the    `!
;;            | :thread      |  job handler if  ;!;
;;            | :last-run    |  the job has     ,!'        cron methods
;;            |______________|  been scheduled  !'    +---------------------+
;;                                             ,'     |                     |
;;                     `!.                   ,,'      |  running?  start!   |
;;                       `!..              ,,''       |  stopped?  stop!    |
;;                         `'!....    ....!''         |                     |
;;                            ''''''''''''            +---------------------+
;;                           cronj thread


(ns cronj.core
  (:require [clj-time.core :as t]
            [clj-time.local :as lt]))

(def ^:dynamic *job-list* (ref {}))
(def ^:dynamic *cronj* (atom {:thread nil
                             :last-run nil}))

(def !required-job-keys [:id :desc :handler :schedule])
(def !optional-job-map {:enabled true})
(def !default-check-interval 50) ;;ms

(defn all-jobs []
  (map deref
       (vals @*job-list*)))

(defn all-job-ids []
  (keys @*job-list*))

(defn job-exists? [job-id]
  (contains? @*job-list* job-id))

(defn job-info [job-id]
  (if (job-exists? job-id)
    (deref (@*job-list* job-id))))

(defn is-job? [job]
  (every? true?
          (map #(contains? job %) !required-job-keys)))

(defn add-job [job]
  (if (is-job? job)
    (dosync
     (alter *job-list* assoc (:id job)
            (atom (into !optional-job-map job))))
    (throw (Exception. "The job is not a valid job"))))

(defn remove-job [job-id]
  (dosync
   (alter *job-list* dissoc job-id)))

(defn remove-all-jobs []
  (dosync
   (alter *job-list* empty)))

(defn $job
  ([job-id k]
     (if-let [job (@*job-list* job-id)]
       (@job k)))
  ([job-id k v]
     (if-let [job (@*job-list* job-id)]
       (swap! job assoc k v)
       (throw (Exception. "The job does not exist")))))

(defn enable [job-id]
  ($job job-id :enabled true))

(defn disable [job-id]
  ($job job-id :enabled false))

(defn enabled? [job-id]
  ($job job-id :enabled))

(def disabled? (comp not enabled?))

(defn toggle [job-id]
  (if (enabled? job-id)
    (disable job-id)
    (enable job-id)))

(defn enabled-jobs []
  (filter #(true? (:enabled %)) (all-jobs)))

(defn enabled-job-ids []
  (map :id (enabled-jobs)))

(defn disabled-jobs []
  (filter #(false? (:enabled %)) (all-jobs)))

(defn disabled-job-ids []
  (map :id (disabled-jobs)))

(defn- run-job* [job-id dtime]
  (cond
    (false? (job-exists? job-id))
    (println "Job (" job-id ") does not exist")

    (disabled? job-id) nil ;;(println "Job (" job-id ") is not enabled")

    :else
    (do
      (try
        (let [job     (job-info job-id)]
          ((:handler job) dtime))
        (catch Exception e (.printStackTrace e))))))

(defn -* [] :*)

(defn --
  ([a b]
     (range a (inc b)))
  ([a b s]
     (range a (inc b) s)))

(defn -| [period]
 (fn [v] (zero? (mod v period))))

(defn- to-time-array [dt]
  (map #(% dt)
       [t/sec t/minute t/hour t/day-of-week t/day t/month t/year]))

(defn- match-entry? [e s]
  (cond (= s :*) true
        (= e s) true
        (fn? s) (s e)
        (sequential? s) (some #(match-entry? e %) s)
        :else false))

(defn- match-cronj? [t-arr c-arr]
  (every? true?
          (map match-entry? t-arr c-arr)))

(defn- cronj-fn [dtime]
  (let [lr (:last-run @*cronj*)
        nr (to-time-array dtime)]
    (if (or (nil? lr) (not= lr nr))
      (do
        (swap! *cronj* assoc :last-run nr)
        ;; (println nr) ;; FOR DEBUGGING
        (doseq [job (all-jobs)]
          (if (match-cronj? nr (:schedule job))
            (future (run-job* (:id job) dtime))))))))

(defn- cronj-loop
  ([] (cronj-loop !default-check-interval))
  ([interval]
     (Thread/sleep interval)
     (cronj-fn (t/now))
     (recur interval)))

(defn- initialized?
  ([] (initialized? @*cronj*))
  ([cr]
    (-> cr :thread nil? not)))

(defn stopped?
  ([] (stopped? @*cronj*))
  ([cr]
     (let [x (:thread cr)]
       (or (nil? x)
           (true? x)
           (future-done? x)
           (future-cancelled? x)))))

(def running? (comp not stopped?))

(defn start! [ & args]
  (cond
    (stopped?)
    (swap! *cronj* assoc :thread
           (future
             (apply cronj-loop args)))
    :else
    (println "The cronj scheduler is already running.")))

(defn stop! []
  (if-not (stopped?)
    (swap! *cronj* update-in [:thread] future-cancel)
    (println "The cronj scheduler is already stopped.")))
