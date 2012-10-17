(ns cronj.loop
  (:require [clj-time.core :as t]
            [clj-time.local :as lt]
            [cronj.data.tab :as tab]
            [cronj.global :as g]))

(defn- time-loop-fn [lp]
  (let [last-arr    (@lp :last-check)
        current-time (lt/local-now)
        current-arr  (tab/to-dt-arr current-time)]
    (cond
      (or (not= last-arr current-arr)
          (nil? last-arr))
      (do
        (swap! lp assoc :last-check-time current-time)
        (swap! lp assoc :last-check current-arr))

      :else
      (let [interval (@lp :interval)
            sleep-time (- 1000
                          (t/milli current-time)
                          interval)]
        (if (< 0 sleep-time)
          (Thread/sleep sleep-time)
          (Thread/sleep interval))))
    (recur lp)))

(defn stopped? [lp]
  (let [x (:thread @lp)]
    (or (nil? x)
        (true? x)
        (future-done? x)
        (future-cancelled? x))))

(def running? (comp not stopped?))

(defn start!
  ([lp] (start! lp g/DEFAULT-INTERVAL))
  ([lp interval]
    (cond
      (stopped? lp)
      (do (swap! lp assoc :interval interval)
          (swap! lp assoc :thread (future (time-loop-fn lp))))
      :else
      (println "The cronj loop is already running."))))

(defn stop! [lp]
  (if-not (stopped? lp)
    (swap! lp update-in [:thread] future-cancel)
    (println "The cronjloop is already stopped.")))

(defn restart!
  ([lp] (restart! lp g/DEFAULT-INTERVAL))
  ([lp interval]
    (stop! lp)
    (start! lp interval)))
