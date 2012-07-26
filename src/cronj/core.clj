;;   Diagram
;;
;;                                        service-list
;;                __...--+----+----+----+----+----+----+----+----+
;;       _..---'""      _|.--"|    |    |    |    |    |    |    |
;;      +-------------+'_+----+----+----+----+----+----+----+----+
;;      | :id         |-     /                                 |
;;      | :desc       |     /     dpL                       service methods
;;      | :handler    |    /    ,XPXYb.               +-------------------------+
;;      | :schedule   |   /    dP']X[`XL              | add-(all)-service(s)    |
;;      | :enabled    |  /    `'  ]X[  "              | remove-(all)-service(s) |
;;      |             | /         ]X[                 | enable(d?)              |
;;      |             |/          ]X[                 | disable(d?)             |
;;      +-------------+           ]X[                 | toggle                  |
;;        service               ....:.....            | enabled-service(-id)s   |
;;                         ...!''''''''''!..          | disabled-service(-id)s  |
;;                       ,,'''            '`!.        | $                       |
;;                      .!'    every second  `!.      +-------------------------+
;;            ................  looks at the   !.
;;            | status map   |  service list    `!.
;;            |              |  triggers the     `!
;;            | :thread      |  handler if       ;!;
;;            | :last-run    |  the service has  ,!'        cron methods
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

(def ^:dynamic *service-list* (ref {}))
(def ^:dynamic *cronj* (atom {:thread nil
                             :last-run nil}))

(def !required-service-keys [:id :desc :handler :schedule])
(def !optional-service-map {:enabled true})
(def !default-check-interval 50) ;;ms

(defn all-services []
  (map deref
       (vals @*service-list*)))

(defn all-service-ids []
  (keys @*service-list*))

(defn service-exists? [service-id]
  (contains? @*service-list* service-id))

(defn service-info [service-id]
  (if (service-exists? service-id)
    (deref (@*service-list* service-id))))

(defn is-service? [service]
  (every? true?
          (map #(contains? service %) !required-service-keys)))

(defn add-service [service]
  (if (is-service? service)
    (dosync
     (alter *service-list* assoc (:id service)
            (atom (into !optional-service-map service))))
    (throw (Exception. "The service is not a valid service"))))

(defn remove-service [service-id]
  (dosync
   (alter *service-list* dissoc service-id)))

(defn remove-all-services []
  (dosync
   (alter *service-list* empty)))

(defn $
  ([service-id k]
     (if-let [service (@*service-list* service-id)]
       (@service k)))
  ([service-id k v]
     (if-let [service (@*service-list* service-id)]
       (swap! service assoc k v)
       (throw (Exception. "The service does not exist")))))

(defn enable [service-id]
  ($ service-id :enabled true))

(defn disable [service-id]
  ($ service-id :enabled false))

(defn enabled? [service-id]
  ($ service-id :enabled))

(def disabled? (comp not enabled?))

(defn toggle [service-id]
  (if (enabled? service-id)
    (disable service-id)
    (enable service-id)))

(defn enabled-services []
  (filter #(true? (:enabled %)) (all-services)))

(defn enabled-service-ids []
  (map :id (enabled-services)))

(defn disabled-services []
  (filter #(false? (:enabled %)) (all-services)))

(defn disabled-service-ids []
  (map :id (disabled-services)))

(defn- run-service* [service-id dtime]
  (cond
    (false? (service-exists? service-id))
    (println "Job (" service-id ") does not exist")

    (disabled? service-id) nil ;;(println "Job (" service-id ") is not enabled")

    :else
    (do
      (try
        (let [service   (service-info service-id)
              handler   (:handler service)]
          (handler dtime))
        (catch Exception e (.printStackTrace e))))))

(defn -* [] :*)

(defn --
  ([s] 
    (fn [v] (zero? (mod v period))))
  ([a b]
     (range a (inc b)))
  ([a b s]
     (range a (inc b) s)))

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
        (doseq [service (all-services)]
          (if (match-cronj? nr (:schedule service))
            (future (run-service* (:id service) dtime))))))))

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

(defn start! [& args]
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
