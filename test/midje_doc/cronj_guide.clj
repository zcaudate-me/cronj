(ns midje-doc.cronj-guide
  (:require [cronj.core :refer :all]
            [midje.sweet :refer :all]))

[[:chapter {:title "Installation"}]]

"Add to `project.clj` dependencies:

`[im.chit/cronj `\"`{{PROJECT.version}}`\"`]`"

"All functions are in the `cronj.core` namespace."

[[{:numbered false}]]
(comment (use 'cronj.core))


[[:chapter {:title "Background"}]]

"
`cronj` was built for a project of mine back in 2012. The system needed to record video footage from multiple ip-cameras in fifteen minute blocks, as well as to save pictures from each camera (one picture every second). All saved files needed a timestamp allowing for easy file management and retrieval.

At that time, `quartzite`, `at-at` and `monotony` were the most popular options. After coming up with a list of design features and weighing up all options, I decided to write my own instead. As a core component of the original project, `cronj` has been operational now since October 2012. A couple of major rewrites and api rejuggling were done, but the api has been very stable from version `0.6` onwards.

There are now many more scheduling libraries in the clojure world:

  - [at-at](https://github.com/overtone/at-at)
  - [chime](https://github.com/james-henderson/chime)
  - [clj-cronlike](https://github.com/kognate/clj-cronlike)
  - [cron4j](http://www.sauronsoftware.it/projects/cron4j)
  - [monotony](https://github.com/aredington/monotony)
  - [quartzite](https://github.com/michaelklishin/quartzite)
  - [schejulure](https://github.com/AdamClements/schejulure)

With so many options, and so many different ways to define task schedules, why choose `cronj`? I have listed a number of [design](#design) decisions that make it beneficial. However, for those that are impatient, cut to the chase, by skipping to the [simulations](#running-simulations) section.

"

[[:chapter {:title "Design"}]]

"`cronj` was built around a concept of a **task**. A task has two components:
- A `handler` (what is to be done)
- A `schedule` (when it should be done)

Tasks are *triggered* by a `scheduler` who in-turn is notified of the current time by a `timer`. If a task was scheduled to run at that time, it's `handler` would be run in a seperate thread.
"

[[{:numbered false}]]
(comment
; cronj                schedule
; --------------       +-------------------------+
; scheduler watches    |  '* 8 /2 7-9 2,3 * *'   |
; the timer and        +-------------------------+
; triggers tasks       |  :sec    [:*]           |
; to execute at        |  :min    [:# 8]         |
; the scheduled time   |  :hour   [:| 2]         |
;                      |  :dayw   [:- 7 9]       |
;                      |  :daym   [:# 2] [:# 3]  |
;                      |  :month  [:*]           |
;                      |  :year   [:*]           |
;                      +-----------+-------------+
; task                              |                        XXXXXXXXX
; +-----------------+   +-----------+-----+                XX         XX
; |:id              |   |           |     |\             XX  timer      XX
; |:desc            +---+-+:task    |     | \           X                 X
; |:handler         |   |  :schedule+     |  \         X     :start-time   X
; |:pre-hook        |   |  :enabled       | entry      X     :thread       X+----+
; |:post-hook       |   |  :opts          |    `.      X     :last-check   X     |
; |:enabled         |   |                 |      \      X    :interval    X      |
; |:args            |   _-------._--------,       \      XX             XX       |
; |:running         |    `-._     `..      `.      \       XX         XX         +
; |:last-exec       |        `-._    `-._    `.     \        XXXXXXXXX         watch
; |:last-successful |            `-._    `-._  `.    `.                          +
; +----------+------+                `-._    `-. `.    \                         |
;                         +----+----+----`-._-+-`-.`.--->----+----+----+----+----+----+
;                         |    |    |    |   `-..  | `. |    |    |    |    |    |    |
;                         +----+----+----+----+--`-.---'+----+----+----+----+----+----+
;                                                                          scheduler
)


[[:section {:title "Seperation of Concerns"}]]
"
A task handler is just a function taking two arguments:
"
[[{:numbered false}]]
(comment
   (fn [t opts]
      (... perform a task ...)))

"
**`t`** represents the time at which the handler was called. This solves the problem of *time synchronisation*. For example, I may have three tasks scheduled to run at a same time:

   - perform a calculation and write the result to the database
   - perform a http call and write result to the database
   - load some files, write to single output then store file location to the database.

All these tasks will end at different times. To retrospectively reasoning about how all three tasks were synced, each handler is required to accept the triggred time `t` as an argument.

**`opts`** is a hashmap, for example `{:path '/app/videos'}`. It has been found that user customisations such as server addresses and filenames, along with job schedules are usually specified at the top-most tier of the application whilst handler logic is usually in the middle-tier. Having an extra `opts` argument allow for better seperation of concerns and more readable code.

"

[[:section {:title "Thread Management"}]]
"
In reviewing other scheduling libraries, it was found that fully-featured thread management capabilities were lacking. `cronj` was designed with these features in mind:

- tasks can be triggered to start manually at any time.
- tasks can start at the next scheduled time before the previous thread has finished running so that multiple threads can be running simultaneously for a single task.
- *pre-* and *post-* hooks can be defined for better seperation of setup/notification/cleanup code from handler body.
- running threads can be listed.
- normal and abnormal termination:
    - kill a running thread
    - kill all running threads in a task
    - kill all threads
    - disable task but let running threads finish
    - stop timer but let running threads finish
    - shutdown timer, kill all running threads
"

[[:section {:title "Simulation Testing"}]]

"
Because the `timer` and the `scheduler` modules have been completely decoupled, it was very easy to add a simulation component into `cronj`. Simulation has some very handy features:
- Simulate how the entire system would behave over a long periods of time
- Generating test inputs for other applications.
- Both single and multi-threaded execution strategies are supported.
"

[[:chapter {:title "Walkthrough"}]]

"In this section all the important and novel features and use cases for `cronj` will be shown. Interesting examples include: [simulation](#running-simulations), [task management](#task-management) and [hooks](#hooks)."

[[:section {:title "Creating a Task"}]]

"`print-handler` outputs the value of `{:output opts}` and the time `t`."

[[{:numbered false}]]
(defn print-handler [t opts]
  (println (:output opts) ": " t))

"`print-task` defines the actual task to be run. Note that it is just a map.
- `:handler` set to `print-handler`
- `:schedule` set for task to run every `2` seconds
- `:opts` can be customised
"

[[{:numbered false}]]
(def print-task
  {:id "print-task"
   :handler print-handler
   :schedule "/2 * * * * * *"
   :opts {:output "Hello There"}})

[[:section {:title "Running Tasks"}]]

"Once the task is defined, `cronj` is called to create the task-scheduler (`cj`)."

[[{:numbered false}]]
(def cj (cronj :entries [print-task]))

"Calling `start!` on `cj` will start the timer and `print-handler` will be triggered every two seconds. Calling `stop!` on `cj` will stop all outputs"

[[{:numbered false}]]
(comment
  (start! cj)

  ;; > Hello There :  #<DateTime 2013-09-29T14:42:54.000+10:00>

         .... wait 2 secs ...

  ;; > Hello There :  #<DateTime 2013-09-29T14:42:56.000+10:00>

         .... wait 2 secs ...

  ;; > Hello There :  #<DateTime 2013-09-29T14:42:58.000+10:00>

         .... wait 2 secs ...

  ;; > Hello There :  #<DateTime 2013-09-29T14:43:00.000+10:00>

  (stop! cj))

[[:section {:title "Running Simulations"}]]

"Simulations are a great way to check your application for errors as they provide constant time inputs. This allows an entire system to be tested for correctness. How `simulate` works is that it decouples the `timer` from the `scheduler` and tricks the `scheduler` to trigger on the range of date inputs provided."

[[:subsection {:title "Y2K Revisited"}]]
"For instance, we wish to test that our `print-handler` method was not affected by the Y2K Bug. `T1` and `T2` are defined as start and end times:"

[[{:numbered false}]]
(def T1 (local-time 1999 12 31 23 59 58))

(def T2 (local-time 2000 1  1  0  0 2))

"We can simulate events by calling `simulate` on `cj` with a start and end time. The function will trigger registered tasks to run beginning at T1, incrementing by 1 sec each time until T2. Note that in this example, there are three threads created for `print-handler`. The printed output may be out of order because of indeterminancy of threads (we can fix this later)."
[[{:numbered false}]]
(comment
  (simulate cj T1 T2)

             .... instantly ...

  ;; > Hello There :  #<DateTime 1999-12-31T23:59:58.000+11:00>
  ;; > Hello There :  #<DateTime 2000-01-01T00:00:02.000+11:00>    ;; out of order
  ;; > Hello There :  #<DateTime 2000-01-01T00:00:00.000+11:00>
             )

[[:subsection {:title "Single Threaded"}]]
"To keep ordering of the `println` outputs, `simulate-st` can be used. This will run `print-handler` calls on a single thread and so will keep order of outputs. Because of the sequential nature of this type of simulation, it is advised that `simulate-st` be used only if there are no significant pauses or thread blocking in the tasks."
[[{:numbered false}]]
(comment
  (simulate-st cj T1 T2)

             .... instantly ...

  ;; > Hello There :  #<DateTime 1999-12-31T23:59:58.000+11:00>
  ;; > Hello There :  #<DateTime 2000-01-01T00:00:00.000+11:00>
  ;; > Hello There :  #<DateTime 2000-01-01T00:00:02.000+11:00>
             )

[[:subsection {:title "Interval and Pause"}]]
"Two other arguments for `simulate` and `simulate-st` are:
 - the time interval `(in secs)` between the current time-point and the next time-point (the default is 1)
 - the pause `(in ms)` to take in triggering the next time-point (the default is 0)

It can be seen that we can simulate the actual speed of outputs by keeping the interval as 1 and increasing the pause time to 1000ms"

[[{:numbered false}]]
(comment
  (simulate cj T1 T2 1 1000)

  ;; > Hello There :  #<DateTime 1999-12-31T23:59:58.000+11:00>

         .... wait 2 secs ...

  ;; > Hello There :  #<DateTime 2000-01-01T00:00:00.000+11:00>

         .... wait 2 secs ...

  ;; > Hello There :  #<DateTime 2000-01-01T00:00:02.000+11:00>
         )

[[:subsection {:title "Speeding Up"}]]
"In the following example, the interval has been increased to 2 seconds whilst the pause time has decreased to 100ms. This results in a 20x increase in the speed of outputs."
[[{:numbered false}]]
(comment
  (simulate cj T1 T2 2 100)

  ;; > Hello There :  #<DateTime 1999-12-31T23:59:58.000+11:00>

         .... wait 100 msecs ...

  ;; > Hello There :  #<DateTime 2000-01-01T00:00:00.000+11:00>

         .... wait 100 msecs ...

  ;; > Hello There :  #<DateTime 2000-01-01T00:00:02.000+11:00>
         )

"Being able to adjust these simulation parameters are really powerful testing tools and saves an incredible amount of time in development. For example, we can quickly test the year long output of a task that is scheduled to run once an hour very quickly by making the interval 3600 seconds and the pause time to the same length of time that the task takes to finish.

Through simulations, task-scheduling can now be tested and entire systems just got easier to manage and reason about!"

[[:section {:title "Task Management"}]]

"Task management capabilities of `cronj` will be demonstrated by first creating a `cronj` object with two task entries labeled `l1` and `l2` doing nothing but sleeping for a long time:"

[[{:numbered false}]]
(def cj
  (cronj :entries
         [{:id       :l1
           :handler  (fn [dt opts] (Thread/sleep 30000000000000))
           :schedule "/2 * * * * * *"
           :opts {:data "foo"}}
          {:id       :l2
           :handler  (fn [dt opts] (Thread/sleep 30000000000000))
           :schedule "0-2 * * * * * *"
           :opts {:data "bar"}}]))

[[:subsection {:title "Showing Threads"}]]

"
The task will be triggered using the `exec!` command. This is done for play purposes. Normal use would involve calling `get-threads` after `start!` has been called."

[[{:numbered false}]]
(fact
  (get-threads cj :l1)     ;; See that there are no threads running
  => []                    ;;    - :l1 is empty

  (get-threads cj :l2)
  => []                    ;;    - :l2 is empty

  (exec! cj :l1 T1)        ;; Launch :l1 with time of T1

  (get-threads cj :l1)
  => [{:tid T1 :opts {:data "foo"}}]  ;; l1 now has one running thread

  (exec! cj :l1 T2)        ;; Launch :l2 with time of T2

  (get-threads cj :l1)
  => [{:tid T1 :opts {:data "foo"}}   ;; l1 now has two running threads
      {:tid T2 :opts {:data "foo"}}]


  (exec! cj :l2 T2 {:data "new"})     ;; Launch :l2 with time of T2
  (get-threads cj :l2)
  => [{:tid T2 :opts {:data "new"}}]  ;; l2 now has one running thread

  (get-threads cj)     ;; if no id is given, all running threads can be seen
  => [{:id :l1, :running [{:tid T1 :opts {:data "foo"}}
                          {:tid T2 :opts {:data "foo"}}]}
      {:id :l2, :running [{:tid T2 :opts {:data "new"}}]}])

[[:subsection {:title "Killing Threads"}]]

[[{:numbered false}]]
(fact
  (kill! cj :l1 T1)       ;; Kill :l1 thread starting at time T1
  (get-threads cj :l1)
  => [{:opts {:data "foo"}, :tid T2}] ;; l1 now has one running thread

  (kill! cj :l1)          ;; Kill all :l1 threads
  (get-threads cj :l1)
  => []                   ;; l1 now has no threads

  (kill! cj)              ;; Kill everything in cj
  (get-threads cj)
  => [{:id :l1, :running []}    ;; All threads have been killed
      {:id :l2, :running []}])



[[:section {:title "Pre and Post Hooks" :tag "hooks"}]]

"Having pre- and post- hook entries allow additional processing to be done outside of the handler. They also have the same function signature as the task handler. An example below can be seen where data is passed from one handler to another:"

[[{:numbered false}]]
(comment
  (def cj
    (cronj
     :entries [{:id        :hook
                :desc      "This is showing how a hook example should work"
                :handler   (fn [dt opts]
                             (println "In handle, opts:" opts)
                             (Thread/sleep 1000) ;; Do Something
                             :handler-result)
                :pre-hook  (fn [dt opts]
                             (println "In pre-hook," "opts:" opts)
                             (assoc opts :pre-hook :pre-hook-data))
                :post-hook (fn [dt opts]
                             (println "In post-hook, opts: " opts))
                :opts      {:data "stuff"}
                :schedule  "* * * * * * *"}]))

  (exec! cj :hook T1)

  ;; > In pre-hook, opts: {:data stuff}
  ;; > In handle, opts: {:data stuff, :pre-hook :pre-hook-data}

             .... wait 1000 msecs ....

  ;; > In post-hook, opts:  {:data stuff, :pre-hook :pre-hook-data, :result :handler-result}
  )

"As could be seen, the `:pre-hook` function can modify opts for use in the handler function while `:pre-hook` can take the result of the main handler and do something with it. I use it mostly for logging purposes."

[[:file {:src "test/midje_doc/cronj_api.clj"}]]

[[:chapter {:title "End Notes"}]]

"For any feedback, requests and comments, please feel free to lodge an issue on github or contact me directly.

Chris.
"
