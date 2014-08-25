(ns cronj.test-timer
  (:use midje.sweet
        ;;hara.checkers
        )
    (:require [hara.ova :as ova]
              [clj-time.local :as lt]
              [clj-time.core :as t]
              [cronj.data.tab :as tab]
              [cronj.data.timer :as tm] :reload))

(fact "test timer-fn"
  (let [tmr (tm/timer)
        dt      (tab/truncate-ms (lt/local-now))
        dt-array  (tab/to-dt-array dt)
        _   (#'tm/timer-fn tmr false)]
    (fact "last-check should update"
      (:last-check @tmr) => dt-array
      (tab/truncate-ms (:last-check-time @tmr)) => dt)))


(fact "test stop and start functions"
  (let [tmr (tm/timer)]
    (fact "initialization"
      (tm/stopped? tmr) => true
      (tm/running? tmr) => false)

    (tm/start! tmr) (Thread/sleep 10)
    (fact "start!"
      (tm/stopped? tmr) => false
      (tm/running? tmr) => true)

    (tm/stop! tmr) (Thread/sleep 10)
    (fact "stop!"
      (tm/stopped? tmr) => true
      (tm/running? tmr) => false)))
