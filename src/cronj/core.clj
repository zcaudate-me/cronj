;;
;;                                     service-list
;;             __...--+----+----+----+----+----+----+----+----+----+----+----+----+
;;    _..---'""      _|.--"|    |    |    |    |    |    |    |    |    |    |    |
;;   +-------------+'_+----+----+----+----+----+----+----+----+----+----+----+----+
;;   | service     |-     /                                              |
;;   |             |     /                     X                         |
;;   |    :id      |    /                    XXXXX               service-list methods
;;   |    :desc    |   /                    XXXXXXX          +-------------------------+
;;   |  ++:handler |  /                    XXXXXXXXX         | add-service(s)          |
;;   | +++:schedule| /                        XXX            | remove-(all)-service(s) |
;;   | || :enabled |/                         XXX            | enable-(all)-service(s) |
;;   +-++----------+                          XXX            | disable-(all)-service(s)|
;;     ||  ,-.                                XXX            | toggle-(all)-service(s) |
;;     |+-(   ) fn[time]                      XXX            | list-(all)-service(s)   |
;;     |   `-'                                XXX            | list-enabled-services   |
;;    +-------------------------+             XXX            | list-disabled-services  |
;;    |  "* 8 /2 7-9 2,3 * *"   |             XXX            |                         |
;;    +-------------------------+             XXX            | $ (attribute selector)  |
;;    |  :sec    [:*]           |             XXX            | (get/set)-schedule      |
;;    |  :min    [:# 8]         |             XXX            | (get/set)-handler       |
;;    |  :hour   [:| 2]         |          XXXXXXXXX         +-------------------------+
;;    |  :dayw   [:- 7 9]       |        XX         XX
;;    |  :daym   [:# 2] [:# 3]  |      XX             XX
;;    |  :month  [:*]           |     X      cronj      X            cron methods
;;    |  :year   [:*]           |    X                   X   +-------------------------+
;;    +-------------------------+    X     :thread       X   |                         |
;;                                   X     :last-run     X   |  +running?   +start!    |
;;       cronj function               X    :interval    X    |  +stopped?   +stop!     |
;;       --------------                XX             XX     |  +-interval  +-thread   |
;;      At every interval                XX         XX       |  +-last-run             |
;;      looks at service                   XXXXXXXXX         |                         |
;;      list and triggers                                    +-------------------------+
;;      handler functions
;;      for each enabled
;;      service.


(ns cronj.core
  (:use [clojure.string :only [split join]]
        [clojure.java.shell :only [sh]])
  (:require [clj-time.core :as t]
            [clj-time.local :as lt]))

(def ^:dynamic *service-list* (ref {}))
(def ^:dynamic *cronj* (atom {:thread nil
                              :last-run nil
                              :interval nil}))

(def !required-service-keys [:id :desc :handler :schedule])
(def !required-shell-service-keys [:id :desc :cmd :schedule])

(def !optional-service-map {:enabled true})
(def !default-interval 50) ;;ms


 ;; There are 2 different representations of cronj schedule data:
 ;;   string: (for humans)        "   *       2,4      2-9         /8      ...  "
 ;;    array: (for efficiency)    [ (-*)   [2 4 6]   (-- 2 9)   (-- 8)     ...  ]
 ;;
 ;;            :schedule                 :schedule-arr
 ;;           +---------+                 +---------+
 ;;           |         |                 |         |
 ;;           |         |                 |         |
 ;;           | string  |    -----------> |  array  |
 ;;           |         |    parse-str    |         |
 ;;           |         |                 |         |
 ;;           +---------+                 +---------+
 ;;            for human                    used in
 ;;            use to add                  cron-loop
 ;;            services


;; Methods for type conversion
(defn to-int [x] (Integer/parseInt x))

;; Array Representation
(defn -* [] :*)

(defn --
  ([s]     (fn [v] (zero? (mod v s))))
  ([a b]   (range a (inc b)))
  ([a b s] (range a (inc b) s)))

;; String to Array Methods
(defn- parse-elem-str [es]
  (cond (= es "*") :*
        (re-find #"^\d+$" es) (to-int es)
        (re-find #"^/\d+$" es) (-- (to-int (.substring es 1)))
        (re-find #"^\d+-\d+$" es)
        (apply --
               (sort (map to-int (split es #"-"))))))

(defn- parse-elemlist-str [s]
  (let [e-toks (re-seq #"[^,]+" s)]
    (map parse-elem-str e-toks)))

(defn parse-str [s]
  (let [c-toks (re-seq #"[^\s]+" s)
        len-c (count c-toks)]
    (map parse-elemlist-str c-toks)))


;; Service List Methods
(defn- is-service? [service]
  (every? true?
          (map #(contains? service %) !required-service-keys)))

(defn- is-shell-service? [service]
  (every? true?
          (map #(contains? service %) !required-shell-service-keys)))

(defn service-exists? [service-id]
  (contains? @*service-list* service-id))

(defn list-service [service-id]
  (if (service-exists? service-id)
    (deref (@*service-list* service-id))))

(defn list-all-services []
  (map deref
       (vals @*service-list*)))

(defn list-all-service-ids []
  (keys @*service-list*))

(defn sh-handler [cmd output-fn]
  (fn [dtime]
    (output-fn dtime
               (sh "bash" "-c" cmd))))

(defn sh-print [dtime output]
  (println (:out output)))

(defn add-service! [service]
  (let [sch     (:schedule service)
        adj     (assoc service :schedule-arr (parse-str sch))
        id      (:id adj)
        add-fn  #(dosync
                  (alter *service-list* assoc id
                         (atom (into !optional-service-map %))))]
    (cond (is-service? adj)
          (add-fn service)

          (is-shell-service? adj)
          (add-fn (assoc adj :handler
                         (sh-handler (:cmd adj)
                                     (or (:cmd-fn adj)
                                         sh-print))))

          :else
          (throw (Exception. "The service map is not a valid." service)))))

(defn add-service [service]
    (if (service-exists? (:id service))
      (throw (Exception. "There is already a service with the same id"))
      (add-service! service)))

(defn add-services! [& services]
  (doseq [s services] (add-service! s)))

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

(defn doto-all-services [f]
  (doseq [id (list-all-service-ids)] (f id)))

(defn enable-service [service-id]
  ($ service-id :enabled true))

(defn enable-all-services []
  (doto-all-services enable-service))

(defn disable-service [service-id]
  ($ service-id :enabled false))

(defn disable-all-services []
  (doto-all-services disable-service))

(defn service-enabled? [service-id]
  ($ service-id :enabled))

(def service-disabled? (comp not service-enabled?))

(defn toggle-service [service-id]
  (if (service-enabled? service-id)
    (disable-service service-id)
    (enable-service service-id)))

(defn toggle-all-services []
  (doto-all-services toggle-service))

(defn list-enabled-services []
  (filter #(true? (:enabled %)) (list-all-services)))

(defn list-enabled-service-ids []
  (map :id (list-enabled-services)))

(defn list-disabled-services []
  (filter #(false? (:enabled %)) (list-all-services)))

(defn list-disabled-service-ids []
  (map :id (list-disabled-services)))

(defn- run-service* [service-id dtime]
  (cond
    (false? (service-exists? service-id))
    (println "Job (" service-id ") does not exist")

    (service-disabled? service-id) nil ;;(println "Job (" service-id ") is not enabled")

    :else
    (do
      (try
        (let [service   (list-service service-id)
              handler   (:handler service)]
          (handler dtime))
        (catch Exception e (.printStackTrace e))))))

(defn $
  ([service-id k]
     (if-let [service (@*service-list* service-id)]
       (@service k)))
  ([service-id k v]
     (if-let [service (@*service-list* service-id)]
       (swap! service assoc k v)
       (throw (Exception. "The service does not exist")))))

(defn get-desc [service-id] ($ service-id :desc))
(defn set-desc [service-id desc] ($ service-id :desc desc))

(defn get-handler [service-id] ($ service-id :handler))
(defn set-handler [service-id f] ($ service-id :handler f))

(defn get-schedule [service-id] ($ service-id :schedule))
(defn set-schedule [service-id sch]
  ($ service-id :schedule sch)
  ($ service-id :schedule-arr (parse-str sch)))


;; Cron Thread Methods
(defn- to-time-array [dt]
  (map #(% dt)
       [t/sec t/minute t/hour t/day-of-week t/day t/month t/year]))

(defn- match-array-entry? [te ce]
  (cond (= ce :*) true
        (= te ce) true
        (fn? ce) (ce te)
        (sequential? ce) (some #(match-array-entry? te %) ce)
        :else false))

(defn- match-array? [t-arr c-arr]
  (every? true?
          (map match-array-entry? t-arr c-arr)))

(defn- cronj-service-fn [dtime]
  (let [lr (:last-run @*cronj*)
        nr (to-time-array dtime)]
    (if (or (nil? lr) (not= lr nr))
      (do
        (swap! *cronj* assoc :last-run nr)
        ;;(println nr) ;; FOR DEBUGGING
        (doseq [service (list-all-services)]
          (if (match-array? nr (:schedule-arr service))
            (future (run-service* (:id service) dtime))))))))

(defn stopped?
  ([] (stopped? @*cronj*))
  ([cr]
     (let [x (:thread cr)]
       (or (nil? x)
           (true? x)
           (future-done? x)
           (future-cancelled? x)))))

(def running? (comp not stopped?))

(defn $-thread [] (:thread @*cronj*))
(defn $-last-run [] (:last-run @*cronj*))
(defn $-interval
  ([] (:interval @*cronj*))
  ([interval] (swap! *cronj* assoc :interval interval)))

(defn- cronj-loop []
  (Thread/sleep (:interval @*cronj*))
  (cronj-service-fn (t/now))
  (recur))

(defn start!
  ([] (start! !default-interval))
  ([interval]
    (cond
      (stopped?)
      (do ($-interval interval)
          (swap! *cronj* assoc :thread (future (cronj-loop))))

      :else
      (println "The cronj scheduler is already running."))))

(defn stop! []
  (if-not (stopped?)
    (swap! *cronj* update-in [:thread] future-cancel)
    (println "The cronj scheduler is already stopped.")))

(defn restart! [& args]
  (stop!)
  (apply start! args))
