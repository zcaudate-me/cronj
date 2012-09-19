(ns examples.scheduling
  (:require [cronj.task :as ct]
            [cronj.core :as cj] :reload))

(cj/schedule-task! {:id 1 :desc 1 :handler #(println "Task 1: " %) :tab "0-60/4 * * * * * *"})
(cj/schedule-task! (cronj.task/new 2 "2" #(println "Task 2: " %) :tab "1-60/4 * * * * * *"))

(cj/schedule-task! {:id 3 :desc 3 :handler #(println "Task 3: " %)} "2-60/4 * * * * * *")
(cj/schedule-task! (cronj.task/new 4 "4" #(println "Task 4: " %)) "3-60/4 * * * * * *")

(cj/start!)
(cj/stop!)
