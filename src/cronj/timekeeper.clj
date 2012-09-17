(ns cronj.timekeeper
  (:require [hara.data.dyna :as d]
            [clj-time.core :as t]
            [clj-time.local :as lt]
            [cronj.tab :as tab]
            [cronj.task :as task]
            [cronj.timesheet :as ts] :reload))

(def DEFAULT-INTERVAL 1) ;;ms

(defn new
  ([] (cronj.timekeeper/new (cronj.timesheet/new)))
  ([ts]
     (atom {:thread nil
            :last-check nil
            :interval nil
            :timesheet ts})))

(defn- time-loop [kp]
  (let [last-arr    (@kp :last-check)
        current-time (lt/local-now)
        current-arr  (tab/to-dt-arr current-time)]
    (cond
      (nil? last-arr)
      (swap! kp assoc :last-check current-arr)

      (not= last-arr current-arr)
      (do
        (swap! kp assoc :last-check current-arr)
        (ts/trigger-matched! (:timesheet @kp) current-time current-arr))

      :else
      (let [interval (@kp :interval)
            sleep-time (- 1000
                          (t/milli current-time)
                          interval)]
        (if (< 0 sleep-time)
          (Thread/sleep sleep-time)
          (Thread/sleep interval))))
    (recur kp)))

(defn stopped? [kp]
  (let [x (:thread @kp)]
    (or (nil? x)
        (true? x)
        (future-done? x)
        (future-cancelled? x))))

(def running? (comp not stopped?))

(defn start!
  ([kp] (start! kp DEFAULT-INTERVAL))
  ([kp interval]
    (cond
      (stopped? kp)
      (do (swap! kp assoc :interval interval)
          (swap! kp assoc :thread (future (time-loop kp))))
      :else
      (println "The timekeeper is already running."))))

(defn stop! [kp]
  (if-not (stopped? kp)
    (swap! kp update-in [:thread] future-cancel)
    (println "The timekeeper is already stopped.")))

(defn restart!
  ([kp] (restart! kp DEFAULT-INTERVAL))
  ([kp interval]
    (stop! kp)
    (start! kp interval)))
