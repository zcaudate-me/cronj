(ns cronj.data.timer
  (:require [clj-time.core :as t]
            [clj-time.local :as lt]
            [cronj.data.tab :as tab]))

(def DEFAULT-INTERVAL 1)

(defn timer [& [interval]]
  (atom {:thread nil
         :last-check nil
         :last-check-time nil
         :interval (or interval DEFAULT-INTERVAL)}))

(defn- timer-fn [timer recur?]
  (let [last-arr    (@timer :last-check)
        current-time (lt/local-now)
        current-arr  (tab/to-dt-arr current-time)]
    (cond
      (or (not= last-arr current-arr)
          (nil? last-arr))
      (do
        (swap! timer assoc :last-check-time current-time)
        (swap! timer assoc :last-check current-arr))

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

(defn start!
  ([timer] (start! timer DEFAULT-INTERVAL))
  ([timer interval]
    (cond
      (stopped? timer)
      (do (swap! timer assoc :interval interval)
          (swap! timer assoc :thread (future (timer-fn timer true))))
      :else
      (println "The timer is already running."))))

(defn stop! [timer]
  (if-not (stopped? timer)
    (swap! timer update-in [:thread] future-cancel)
    (println "The timer is already stopped.")))

(defn restart!
  ([timer] (restart! timer DEFAULT-INTERVAL))
  ([timer interval]
    (stop! timer)
    (start! timer interval)))
