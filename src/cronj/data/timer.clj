(ns cronj.data.timer
  (:require [clj-time.core :as t]
            [clj-time.local :as lt]
            [cronj.data.tab :as tab]
            [clojure.tools.logging :as log]))

(def DEFAULT-INTERVAL 1)

(defn timer [& [interval]]
  (atom {:thread nil
         :start-time nil
         :last-check nil
         :last-check-time nil
         :interval (or interval DEFAULT-INTERVAL)}))

(defn- timer-fn [timer recur?]
  (let [last-array    (@timer :last-check)
        current-time (lt/local-now)
        current-array  (tab/to-dt-array current-time)]
    (cond
      (or (not= last-array current-array)
          (nil? last-array))
      (swap! timer assoc
             :last-check-time current-time
             :last-check current-array)

      :else
      (let [interval (@timer :interval)
            sleep-time (- 1000
                          (t/milli current-time)
                          interval)]
        (if (< 0 sleep-time)
          (Thread/sleep sleep-time)
          (Thread/sleep interval))))
    (if recur?
      (recur timer true))))

(defn stopped? [timer]
  (let [x (:thread @timer)]
    (or (nil? x)
        (true? x)
        (future-done? x)
        (future-cancelled? x))))

(def running? (comp not stopped?))

(defn uptime [timer]
  (let [start   (:start-time @timer)
        current (:last-check-time @timer)]
    (if (and start current)
      (- (clj-time.coerce/to-long current)
         (clj-time.coerce/to-long start)))))

(defn start!
  ([timer] (start! timer DEFAULT-INTERVAL true))
  ([timer interval recur?]
    (cond
      (stopped? timer)
      (swap! timer assoc
             :start-time (lt/local-now)
             :interval interval
             :thread (future (timer-fn timer recur?)))
      :else
      (log/info "The timer is already running."))))

(defn trigger!
  [timer] (start! timer DEFAULT-INTERVAL false))

(defn stop! [timer]
  (if-not (stopped? timer)
    (swap! timer (fn [m]
                   (-> (update-in m [:thread] future-cancel)
                       (assoc :thread nil
                              :start-time nil))))
    (log/info "The timer is already stopped.")))

(defn restart!
  ([timer] (restart! timer DEFAULT-INTERVAL))
  ([timer interval]
    (stop! timer)
    (start! timer interval)))
