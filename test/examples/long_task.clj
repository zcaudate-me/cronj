(ns examples.long-task
  (:use clojure.pprint)
  (:require [cronj.task :as ct]
            [cronj.core :as cj] :reload))

(cj/unschedule-all-tasks!)
(cj/schedule-task!
 {:id :30s-task
  :desc "This is a 30 second task"
  :handler (fn [_] (Thread/sleep 30000))
  :tab "/5 * * * * * *"})

(cj/stop!)
(cj/start!)
(println (cj/running?))
(pprint (cj/list-running-for-task :30s-task))
(pprint (cj/last-exec-for-task :30s-task))
(pprint (cj/last-successful-for-task :30s-task))
(pprint [(cj/last-exec-for-task :30s-task)
         (cj/last-successful-for-task :30s-task)])
(cj/kill-all-running-for-task! :30s-task)
(cj/list-all-tasks)
(cj/list-all-task-ids)
