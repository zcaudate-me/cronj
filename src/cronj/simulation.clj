(ns cronj.simulation
  (:require [hara.ova :as ova]
            [clj-time.core :as t]
            [clj-time.local :as lt]
            [cronj.data.tab :as tab]
            [cronj.data.scheduler :as ts]))

(defn local-time [& args]
  (lt/to-local-date-time
   (apply t/date-time args)))

;; Speed Up Execution.

(defn- simulate-loop [cnj start end interval pause]
  (if-not (t/before? end start)
    (do
      (ts/signal-tick (:scheduler cnj) start)
      (if pause (Thread/sleep pause))
      (recur cnj (t/plus start interval) end interval pause))))

(defn simulate [cnj start end & [interval pause]]
  (let [interval (cond (nil? interval)
                       (t/seconds 1)

                       (integer? interval)
                       (t/seconds interval)

                       :else interval)
        pause    (or pause 0)]
    (simulate-loop cnj start end interval pause)))

;; Single Threaded Simulation.

(defn exec-st [task dt]
  (let [pre       (:pre-hook task)
        post      (:post-hook task)
        handler   (:handler (:task task))
        opts      (or (:opts task) {})
        exec-hook (fn [hook dt opts]
                    (if (fn? hook) (hook dt opts) opts))
        fopts  (exec-hook pre dt opts)
        result (handler dt fopts)]
    (exec-hook post dt (assoc fopts :result result))))

(defn- simulate-st-loop [cnj start end interval pause]
  (if-not (t/before? end start)
    (let [dt-array (tab/to-dt-array start)]
      (doseq [entry (ova/select (:scheduler cnj) [:enabled true])]
        (if (tab/match-array? dt-array (:tab-array entry))
          (exec-st entry start)))
      (if pause (Thread/sleep pause))
      (recur cnj (t/plus start interval) end interval pause))))

(defn simulate-st [cnj start end & [interval pause]]
  (let [interval (cond (nil? interval)
                       (t/seconds 1)

                       (integer? interval)
                       (t/seconds interval)

                       :else interval)
        pause    (or pause 0)]
    (simulate-st-loop cnj start end interval pause)))
