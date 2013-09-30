(ns midje-doc.cronj-api
  (:require [cronj.core :refer :all]
            [midje.sweet :refer :all]))

[[:chapter {:title "API Reference"}]]

[[:section {:title "cronj"}]]

"`cronj` constructs a task-scheduler object."

[[{:numbered false}]]
(comment
  (cronj :entries <vector-of-tasks>))

"An simple example:"

[[{:numbered false}]]
(def cnj
  (cronj :entries
         [{:id "print-task"
           :handler (fn [t opts] (println (:output opts) ": " t))
           :schedule "/2 * * * * * *"
           :opts {:output "Hello There"}}]))

[[:subsection {:title "crontab"}]]

"Each `cronj` task has a `:schedule` entry. The value is a string specifying when it is supposed to run. The string is of the same format as `crontab` -  seven elements seperated by spaces. The elements are used to match the time, expressed as seven numbers:

     second minute hour day-of-week day-of-month month year

The rules for a match between the crontab and the current time are:

- `A`       means match on `A`
- `*`       means match on any number
- `E1,E2`   means match on both `E1` and `E2`
- `A-B`     means match on any number between `A` and `B` inclusive
- `/N`      means match on any number divisible by `N`
- `A-B/N`   means match on any number divisible by `N` between `A` and `B` inclusive

Where `A`, `B` and `N` are numbers; `E1` and `E2` are expressions. All seven elements in the string have to match in order for the task to be triggered.
"

[[{:numbered false}]]
(comment

  ;; Triggered every 5 seconds

  "/5 * * * * * *"


  ;; Triggered every 5 seconds between 32 and 60 seconds

  "32-60/5 * * * * * *"

  ;; Triggered every 5 seconds on the 9th aand 10th
  ;; minute of every hour on every Friday from June
  ;; to August between years 2012 to 2020.

  "/5  9,10  * 5 * 6-8 2012-2020")


[[:section {:title "system commands"}]]

"System commands mainly work with the cronj timer."

[[:subsection {:title "start!"}]]

"Starts up the timer such that tasks are launched at the scheduled time"

[[{:numbered false}]]
(comment
  (start! <cnj>))

[[:subsection {:title "stop!"}]]

"Stops the timer. New task threads will not be launched. However, exisiting tasks threads will not be killed and finish naturally."

[[{:numbered false}]]
(comment
  (stop! <cnj>))

[[:subsection {:title "shutdown!"}]]

"Stops the timer. New task threads will not be launched. Exisiting tasks threads will be killed immediately"

[[{:numbered false}]]
(comment
  (shutdown! <cnj>))

[[:subsection {:title "restart!"}]]

"Restarts the timer, killing all exisiting threads."

[[{:numbered false}]]
(comment
  (restart! <cnj>))

[[:subsection {:title "stopped?"}]]

"Checks whether the timer is stopped"

[[{:numbered false}]]
(comment
  (stopped? <cnj>))

[[:subsection {:title "running?"}]]

"Checks whether the timer is running. Complement of stopped?"

[[{:numbered false}]]
(comment
  (running? <cnj>))

[[:subsection {:title "uptime"}]]

"Checks how long the timer has been running. Returns a long representing the time in msecs."

[[{:numbered false}]]
(comment
  (uptime <cnj>))

[[:section {:title "task scheduling"}]]

[[:subsection {:title "enable-task"}]]

"If a task has been disabled, meaning that the task will not run at its allocated time, `enable-task` will enable it."

[[{:numbered false}]]
(comment
  (enable-task <cnj> <task-id))

[[:subsection {:title "disable-task"}]]

"Disables a task so that it will not run"

[[{:numbered false}]]
(comment
  (disable-task <cnj> <task-id))

[[:subsection {:title "task-enabled?"}]]

"Checks if a task has been enabled. Tasks are enabled by default"

[[{:numbered false}]]
(comment
  (task-enabled? <cnj> <task-id))

[[:subsection {:title "task-disabled?"}]]

"Checks if a task has been disabled."

[[{:numbered false}]]
(comment
  (task-disabled? <cnj> <task-id))

[[:section {:title "task simulation"}]]

[[:subsection {:title "simulate"}]]

"Simulates the timer over `start-time` and `end-time`. Additional parameters are `interval` and `pause`. More examples can be found in [Running Simulations](#running-simulations)"

[[{:numbered false}]]
(comment
  (simulate <cnj> <start-time> <end-time>)

  (simulate <cnj> <start-time> <end-time> <interval> <pause>))

[[:subsection {:title "simulate-st"}]]

"Simulate the timer over start-time and end-time. Just like `simulate` but all tasks are executed on a single thread (only should be used on non-blocking handlers)"

[[{:numbered false}]]
(comment
  (simulate-st <cnj> <start-time> <end-time>)

  (simulate-st <cnj> <start-time> <end-time> <interval> <pause>))


[[:section {:title "thread management"}]]

[[:subsection {:title "get-ids"}]]

"Return a list of all task ids:"

[[{:numbered false}]]
(comment
  (get-ids <cnj>))

[[:subsection {:title "get-task"}]]

"Return the task entry by id"

[[{:numbered false}]]
(comment
  (get-task <cnj> <task-id>)

  ;; Example Output:
  (get-task cnj "print-task")
  => {:task {:desc ""
             :running "<Ova1228148821 []>"
             :last-exec "#<Ref@791c61fe: nil>"
             :last-successful "#<Ref@3665a8d0: nil>"
             :handler "#<cronj_api$fn__8574 midje_doc.cronj_api$fn__85744c2e0b96>"
             :id "print-task"}
      :schedule "/2 * * * * * *"
      :tab-array (("<tab$_STAR__$fn__2780 cronj.data.tab$_STAR__$fn__278062facbec>")
                  (:*) (:*) (:*) (:*) (:*) (:*))
      :enabled true
      :opts {:output "Hello There"}
      :output "#<Atom@3f6225b8: nil>"})

[[:subsection {:title "get-threads"}]]

"Returns a list of running threads. See [Task Management](#task-management) for examples."

[[{:numbered false}]]
(comment
  (get-threads <cnj>)  ;; Gets all running threads in <cnj>

  (get-threads <cnj> <task-id>)  ;; Gets all threads for <task-id> in <cnj>
)

[[:subsection {:title "exec!"}]]

"Launches a thread for the task, irrespective of whether the task has not been scheduled or that it has been disabled."

[[{:numbered false}]]
(comment
  (exec! <cnj> <task-id>)  ;; launches a new thread for <task-id> using
                           ;; the current time and default opts

  (exec! <cnj> <task-id> <dt>)  ;; launches a new thread for <task-id> using
                                ;; the time as <dt> and default opts

  (exec! <cnj> <task-id> <dt> <opts>) ;; launches a new thread for <task-id> using
                                      ;; the time as <dt> and opts as <opts>
)

[[:subsection {:title "kill!"}]]

"Kills running threads. See [Task Management](#task-management) for examples."

[[{:numbered false}]]
(comment
  (kill! <cnj>)    ;; Kills all running threads

  (kill! <cnj> <task-id>)  ;; Kills all running threads for <task-id>

  (kill! <cnj> <task-id> <dt>) ;; Kills only thread started at <dt> for <task-id>
)
