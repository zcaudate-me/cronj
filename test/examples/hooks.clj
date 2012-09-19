(ns examples.hooks
  (:require [cronj.task :as ct]
            [cronj.core :as cj]))

(def tsk
  (ct/new "hook-example"
          "This is showing how a hook example should work"
          (fn [dt & args]
            (println "Doing Something in the Handle, Arguments:" args)
            (Thread/sleep 1000)
            "Result")
          :pre-hook (fn [dt args]
                      (println "Doing Something in Pre-Hook, Arguments: " args)
                      (Thread/sleep 1000)
                      (assoc args :pre "Hooked"))
          :post-hook (fn [dt args]
                       (println "In Post-Hook, Arguments: " args))
          :args {:a "a" :b "b"}
  ;; optional  :tab "/5 * * * * * *")
  )

(ct/exec! tsk 1)

(cj/unschedule-all-tasks!)
(cj/schedule-task!
 {:id        "hook-example"
  :desc      "This is showing how a hook example should work"
  :handler   (fn [dt & args]
               (println "Instance:" dt " - Doing Something in the Handle Arguments:" args)
               (Thread/sleep 1000) ;; Do Something
               "Result")
  :pre-hook  (fn [dt args]
               (println "Instance:" dt " - In Pre-Hook:" "Arguments" args)
               (assoc args :pre "Pre-Hooked"))
  :post-hook (fn [dt args]
               (println  "Instance:" dt " - In Post-Hook, Arguments: " args))
  :args      {:a "a" :b "b"}
  :tab       "/5 * * * * * *"})

(cj/start!)

(cj/stop!)
