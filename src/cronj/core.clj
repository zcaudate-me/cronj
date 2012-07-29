;;
;;                                     task-list
;;             __...--+----+----+----+----+----+----+----+----+----+----+----+----+
;;    _..---'""      _|.--"|    |    |    |    |    |    |    |    |    |    |    |
;;   +-------------+'_+----+----+----+----+----+----+----+----+----+----+----+----+
;;   | task     |-     /                                              |
;;   |             |     /                     X                         |
;;   |    :id      |    /                    XXXXX               task-list methods
;;   |    :desc    |   /                    XXXXXXX          +-------------------------+
;;   |  ++:handler |  /                    XXXXXXXXX         | add-task(s)          |
;;   | +++:schedule| /                        XXX            | remove-(all)-task(s) |
;;   | || :enabled |/                         XXX            | enable-(all)-task(s) |
;;   +-++----------+                          XXX            | disable-(all)-task(s)|
;;     ||  ,-.                                XXX            | toggle-(all)-task(s) |
;;     |+-(   ) fn[time]                      XXX            | list-(all)-task(s)   |
;;     |   `-'                                XXX            | list-enabled-tasks   |
;;    +-------------------------+             XXX            | list-disabled-tasks  |
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
;;      looks at task                   XXXXXXXXX         |                         |
;;      list and triggers                                    +-------------------------+
;;      handler functions
;;      for each enabled
;;      task.


(ns cronj.core
  (:use [clojure.string :only [split join]]
        [clojure.java.shell :only [sh]])
  (:require [clj-time.core :as t]
            [clj-time.local :as lt]))

(def ^:dynamic *task-list* (ref {}))
(def ^:dynamic *cronj* (atom {:thread nil
                              :last-run nil
                              :interval nil}))

(def !required-task-keys [:id :desc :handler :schedule])
(def !required-shell-task-keys [:id :desc :cmd :schedule])

(def !optional-task-map {:enabled true})
(def !default-interval 1) ;;ms


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
 ;;            tasks


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
(defn- is-task? [task]
  (every? true?
          (map #(contains? task %) !required-task-keys)))

(defn- is-shell-task? [task]
  (every? true?
          (map #(contains? task %) !required-shell-task-keys)))

(defn task-exists? [task-id]
  (contains? @*task-list* task-id))

(defn list-task [task-id]
  (if (task-exists? task-id)
    (deref (@*task-list* task-id))))

(defn list-all-tasks []
  (map deref
       (vals @*task-list*)))

(defn list-all-task-ids []
  (keys @*task-list*))

(defn sh-handler [cmd output-fn]
  (fn [dtime]
    (output-fn dtime
               (sh "bash" "-c" cmd))))

(defn sh-print [dtime output]
  (println (:out output)))

(defn add-task! [task]
  (let [sch     (:schedule task)
        adj     (assoc task :schedule-arr (parse-str sch))
        id      (:id adj)
        add-fn  #(dosync
                  (alter *task-list* assoc id
                         (atom (into !optional-task-map %))))]
    (cond (is-task? adj)
          (add-fn adj)

          (is-shell-task? adj)
          (add-fn (assoc adj :handler
                         (sh-handler (:cmd adj)
                                     (or (:cmd-fn adj)
                                         sh-print))))

          :else
          (throw (Exception. "The task map is not a valid." task)))))

(defn add-task [task]
    (if (task-exists? (:id task))
      (throw (Exception. "There is already a task with the same id"))
      (add-task! task)))

(defn add-tasks! [& tasks]
  (doseq [s tasks] (add-task! s)))

(defn remove-task [task-id]
  (dosync
   (alter *task-list* dissoc task-id)))

(defn remove-all-tasks []
  (dosync
   (alter *task-list* empty)))

(defn $
  ([task-id k]
     (if-let [task (@*task-list* task-id)]
       (@task k)))
  ([task-id k v]
     (if-let [task (@*task-list* task-id)]
       (swap! task assoc k v)
       (throw (Exception. "The task does not exist")))))

(defn doto-all-tasks [f]
  (doseq [id (list-all-task-ids)] (f id)))

(defn enable-task [task-id]
  ($ task-id :enabled true))

(defn enable-all-tasks []
  (doto-all-tasks enable-task))

(defn disable-task [task-id]
  ($ task-id :enabled false))

(defn disable-all-tasks []
  (doto-all-tasks disable-task))

(defn task-enabled? [task-id]
  ($ task-id :enabled))

(def task-disabled? (comp not task-enabled?))

(defn toggle-task [task-id]
  (if (task-enabled? task-id)
    (disable-task task-id)
    (enable-task task-id)))

(defn toggle-all-tasks []
  (doto-all-tasks toggle-task))

(defn list-enabled-tasks []
  (filter #(true? (:enabled %)) (list-all-tasks)))

(defn list-enabled-task-ids []
  (map :id (list-enabled-tasks)))

(defn list-disabled-tasks []
  (filter #(false? (:enabled %)) (list-all-tasks)))

(defn list-disabled-task-ids []
  (map :id (list-disabled-tasks)))

(defn- run-task* [task-id dtime]
  (cond
    (false? (task-exists? task-id))
    (println "Job (" task-id ") does not exist")

    (task-disabled? task-id) nil ;;(println "Job (" task-id ") is not enabled")

    :else
    (do
      (try
        (let [task   (list-task task-id)
              handler   (:handler task)]
          (handler dtime))
        (catch Exception e (.printStackTrace e))))))

(defn $
  ([task-id k]
     (if-let [task (@*task-list* task-id)]
       (@task k)))
  ([task-id k v]
     (if-let [task (@*task-list* task-id)]
       (swap! task assoc k v)
       (throw (Exception. "The task does not exist")))))

(defn get-desc [task-id] ($ task-id :desc))
(defn set-desc [task-id desc] ($ task-id :desc desc))

(defn get-handler [task-id] ($ task-id :handler))
(defn set-handler [task-id f] ($ task-id :handler f))

(defn get-schedule [task-id] ($ task-id :schedule))
(defn set-schedule [task-id sch]
  ($ task-id :schedule sch)
  ($ task-id :schedule-arr (parse-str sch)))


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

(defn $-thread [] (:thread @*cronj*))
(defn $-last-run [] (:last-run @*cronj*))
(defn $-interval
  ([] (:interval @*cronj*))
  ([interval] (swap! *cronj* assoc :interval interval)))

(defn- cronj-fn [dtime darray]
  (doseq [task (list-all-tasks)]
    (if (match-array? darray (:schedule-arr task))
      (future (run-task* (:id task) dtime)))))

(defn- cronj-loop []
  (let [last-arr    ($-last-run)
        current-time (t/now)
        current-arr  (to-time-array current-time)]
    (if (or (nil? last-arr) (not= last-arr current-arr))
      (do
        (swap! *cronj* assoc :last-run current-arr)
        (cronj-fn current-time current-arr))
      (let [interval ($-interval)
            sleep-time (- 1000
                          (t/milli current-time)
                          interval)]
        (if (< 0 sleep-time)
          (Thread/sleep sleep-time)
          (Thread/sleep interval))))
    (recur)))


(defn stopped?
  ([] (stopped? @*cronj*))
  ([cr]
     (let [x (:thread cr)]
       (or (nil? x)
           (true? x)
           (future-done? x)
           (future-cancelled? x)))))

(def running? (comp not stopped?))

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
