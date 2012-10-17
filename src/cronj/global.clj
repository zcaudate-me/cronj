(ns cronj.global
  (:require [hara.fn :as f]
            [cronj.data.timesheet :as ts]))

(def DEFAULT-INTERVAL 1)

(def ^:dynamic *timesheet* (ts/timesheet))

(def ^:dynamic *timeloop*
  (atom {:thread nil
         :last-check nil
         :last-check-time nil
         :interval DEFAULT-INTERVAL}))

(defn install-watch [timeloop timesheet]
  (add-watch
   timeloop :time-watch
   (f/watch-for-change
    [:last-check]
    (fn [_ ref _ _]
      (let [r @ref]
        (ts/trigger-matched! timesheet (:last-check-time r) (:last-check r)))))))

;; Adding the watch-fn to the global timeloop. This seperates the polling loop from
;; the actual updating of the. More watches can potentially be installed
;; by third parties -> ie. signalling events, updating databases and interfaces, etc..
(install-watch *timeloop* *timesheet*)
