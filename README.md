# cronj

## Installation:

In project.clj, add to dependencies:

     [cronj "0.6.1"]

## Quick Start

    (require '[cronj.core :as cj])
    (cj/defcronj cnj
      :entries [{:id       :t1
                 :handler  (fn [dt opts] (println "Task 1: " dt))
                 :schedule "0-60/2 * * * * * *"}
                {:id       :t2
                 :handler  (fn [dt opts] (println "Task 2: " dt))
                 :schedule "1-60/2 * * * * * *"}])

    (cj/start! cnj)

        ;; wait for outputs ......
        ;;> Task 1:  #DateTime 2012-09-17T14:10:24.000+10:00
        ;;> Task 2:  #DateTime 2012-09-17T14:10:25.000+10:00
        ;;> Task 1:  #DateTime 2012-09-17T14:10:26.000+10:00
        ;;> Task 2:  #DateTime 2012-09-17T14:10:27.000+10:00
        ;;> Task 1:  #DateTime 2012-09-17T14:10:28.000+10:00

    ;; get bored and stop
    (cj/stop! cnj)

## Introduction
This is *another* cron-inspired task-scheduling library. I have found many scheduling libraries for clojure:

  - [cron4j](http://www.sauronsoftware.it/projects/cron4j)
  - [clj-cronlike](https://github.com/kognate/clj-cronlike)
  - [at-at](https://github.com/overtone/at-at)
  - [monotony](https://github.com/aredington/monotony)
  - [quartzite](https://github.com/michaelklishin/quartzite)

The first two follow the cron convention. The "task" (also called a "job") can only be scheduled at whole minute intervals. [at-at](https://github.com/overtone/at-at) has milli-second resolution, but was limited in the number of threads that have to be predetermined. It was good for looking after tasks that did not overlap between calls but not for tasks that may take an arbitarily long time. [monotony](https://github.com/aredington/monotony) and [quartzite](https://github.com/michaelklishin/quartzite) are both very cool and worth having a look.

Why use `cronj` if there are so many others like it? Well, `cronj` solves a really annoying problem I ran into with my multithreaded applications - "Task Synchronisation".

For example, you have three tasks scheduled to run at a same time: 
   - a task that performs some calculation and writes to the database
   - a task that performs a http call and writes to the database
   - a task that loads a series of files, manipulates them into one file, saves it somewhere, then writes the location to the database.

All these tasks will start and end at different times in a multithreaded environment. If you wanted to retrospectively reason about how all three events may be synced, you want to pass the same time token to each of the task handlers when they are triggered

`cronj` was built to support this style of programming.

The only novelity that `cronj` brings to the table is the fact that when you are defining task handlers, the function that gets triggered whenever have to accept a timestamp of when they are called. This one little addition ends up being really handy and solves a whole class of problems. The timestamp essentially acts as a coordinate that 'syncs' different handlers that are scheduled to trigger at the same time and so it makes coordinating task-handlers in a multithreaded environment less painful.

## Features
  - Easy to Use: `cronj` tasks are defined as maps, schedules are defined as strings. It does get any easier!
  - Easy to Hack: less than 500 lines of code, fully implemented in clojure, minimal dependencies. 
  - Easy to Understand: Functionality is broken up into modular components and are thoroughly tested
     - Task Thread Control (cronj.data.task)
     - Task Scheduling Control (cronj.data.timetable)
     - Timer (cronj.data.timer)
  - Easy to Control
     - Launch tasks with per-second interval having high system-time accuracy without wasting system resourcs.
     - Spawn as many threads as needed. tasks started at earlier can exist alongside tasks started at later.
     - List tasks and running theads
     - Normal and abnormal termination: 
                 kill a running thread
                 kill all running threads in a task
                 kill all threads
                 disable task but let running threads finish
                 stop cronj but let running threads finish
                 shutdown cronj, kill all running threads


### Usage
task customisation options can be added to the handler through the opts argument

    (cj/defcronj cnj
      :entries [{:id "opts"
                 :handler (fn [dt opts] (println (:task-name opts) ": " dt))
                 :schedule "/2 * * * * * *"
                 :opts {:task-name "Hello There"}}])

    (cj/start! cnj)
    
        ;; more outputs ......
        ;;> Hello There :  #DateTime 2012-09-17T14:53:10.000+10:00
        ;;> Hello There :  #DateTime 2012-09-17T14:53:12.000+10:00
        ;;> Hello There :  #DateTime 2012-09-17T14:53:14.000+10:00

    ;; stop outputs
    (cj/stop! cnj)


### More Examples

    (cj/defcronj cnj
      :entries [{:id "every-5-seconds-1"
                 :desc "prints out the date every 5 seconds"
                 :handler (fn [dt opts] (println dt))
                 :schedule "/5 * * * * * *"}])

    (cj/defcronj cnj
      :entries [{:id "every-5-seconds-2"
                 :desc "prints out the date every 5 seconds between 32 and 60 seconds"
                 :handler (fn [dt opts] (println dt))
                 :schedule "32-60/5 * * * * * *"})

    (cj/defcronj cnj
      :entries [{:id "every-5-seconds-2"
                 :desc "prints out the date every 5 seconds on the
                        9th aand 10th minute of every hour on every Friday
                        from June to August between the year 2012 to 2020"
                 :handler (fn [dt opts] (println dt))
                 :schedule "/5  9,10  * 5 * 6-8 2012-2020"})

### Hooks
For additional control like cleanup and other side-effecting operations, post and pre hooks can also be set.

    (cj/defcronj cnj
      :entries [{:id        "hook-example"
                 :desc      "This is showing how a hook example should work"
                 :handler   (fn [dt opts]
                               (println "Instance:" dt "- handle, opts:" opts)
                               (Thread/sleep 1000) ;; Do Something
                               "Result")
                 :pre-hook  (fn [dt opts]
                               (println "Instance:" dt "- pre-hook," "opts:" opts)
                               (assoc opts :pre "Pre-Hooked"))
                 :post-hook (fn [dt opts]
                               (println  "Instance:" dt "- In post-hook, opts: " args))
                 :opts      {:a "sample-a" :b "sample-a"}
                 :schedule  "/5 * * * * * *"})

    (cj/start! cnj)
    ;;> #DateTime 2012-11-21T13:10:40.000+11:00 - pre-hook, opts: {:a sample-a, :b sample-a}
    ;;> #DateTime 2012-11-21T13:10:40.000+11:00 - handle, opts: {:a sample-a, :b sample-a, :pre Pre-Hooked}
    ;;> #DateTime 2012-11-21T13:10:40.000+11:00 - post-hook, opts:  {:a sample-a, :b sample-a, :result Result, :pre Pre-Hooked}

    (cj/stop! cnj)

Thats all really... now go write your own handlers!


### Overview

Cronj is seperated into three basic concepts:

- Tasks are records that provide information about the task and keeps track of the running instances that has been sw.

- A Timer to keep the time.

- A Timetable to strictly schedule and unschedule tasks according to a `tab` schedule as well as to trigger tasks.

<pre>
       cronj                schedule
       --------------       +-------------------------+
       timetable watches    |  "* 8 /2 7-9 2,3 * *"   |
       the timer and        +-------------------------+
       triggers tasks       |  :sec    [:*]           |
       to execute at        |  :min    [:# 8]         |
       the scheduled time   |  :hour   [:| 2]         |
                            |  :dayw   [:- 7 9]       |
                            |  :daym   [:# 2] [:# 3]  |
                            |  :month  [:*]           |
                            |  :year   [:*]           |
                            +-----------+-------------+
      task                              |                        XXXXXXXXX
      +-----------------+   +-----------+-----+                XX         XX
      |:id              |   |           |     |\             XX  timer      XX
      |:desc            +---+-+:task    |     | \           X                 X
      |:handler         |   |  :schedule+     |  \         X     :start-time   X
      |:pre-hook        |   |  :enabled       | entry      X     :thread       X+----+
      |:post-hook       |   |  :opts          |    `.      X     :last-check   X     |
      |:enabled         |   |                 |      \      X    :interval    X      |
      |:args            |   _-------._--------,       \      XX             XX       |
      |:running         |    `-._     `..      `.      \       XX         XX         +
      |:last-exec       |        `-._    `-._    `.     \        XXXXXXXXX         watch
      |:last-successful |            `-._    `-._  `.    `.                          +
      +----------+------+                `-._    `-. `.    \                         |
                              +----+----+----`-._-+-`-.`.--->----+----+----+----+----+----+
                              |    |    |    |   `-..  | `. |    |    |    |    |    |    |
                              +----+----+----+----+--`-.---'+----+----+----+----+----+----+
                                                                                timetable
</pre>


Tasks can be added and removed on the fly through the `cronj` library interface and the library will then keep an eye out on the time. At the correct time that a task has been scheduled to start, the task handler will be launched in another thread. The actual polling loop is quite efficient and will only poll a maximum of twice every second with a 1ms timing error. Future improvements to the loop will hope to preempt the time that tasks should start and sleep until it is necessary to wake up.


### Long running tasks

    (cj/defcronj cnj
      :entries [{:id       :long-running
                 :handler  (fn [dt opts] (Thread/sleep 30000))
                 :schedule "0-60/5 * * * * * *"}
                 :opts {:foo "bar"}])

    (cj/start! cnj)

    (cj/task-threads cnj :long-running)

        ;; list of running threads
        ;;> ({:opts {:foo "bar"}, :tid #<DateTime 2012-11-21T14:03:55.000+11:00>} 
        ;;   {:opts {:foo "bar"}, :tid #<DateTime 2012-11-21T14:04:00.000+11:00>} 
        ;;   {:opts {:foo "bar"}, :tid #<DateTime 2012-11-21T14:04:05.000+11:00>} 
        ;;   {:opts {:foo "bar"}, :tid #<DateTime 2012-11-21T14:04:10.000+11:00>} 
        ;;   {:opts {:foo "bar"}, :tid #<DateTime 2012-11-21T14:04:15.000+11:00>} 
        ;;   {:opts {:foo "bar"}, :tid #<DateTime 2012-11-21T14:04:20.000+11:00>})

    (cj/kill-threads cnj)
    (cj/task-threads cnj :long-running)
        
        ;; threads have been killed
        ;;> () 

    (cj/stop! cnj)

## TODO:

Tests
- Core (More use cases and multi-threaded examples)
- Documentation of cronj.core functions and more use cases


### Breaking Changes!:

#### v0.6.1

Quite a big overhaul following principles popularised by [Misko Hevery](http://misko.hevery.com/) through his videos on [google tech talks](https://www.google.com/search?btnG=1&pws=0&q=misko+hevery+google+tech+talks)
- using hara 0.6.1 for a shared array structure, which is fully STM conversant.
- got rid of all global objects
- a `defcronj` macro for more declarative style of programming
- cleaner seperation of internal functionality so that all components can be tested individually
- the handler now has to take a date-time AND options arguments
- Tests written for all library code:
        - cronj.data.tab
        - cronj.data.task
        - cronj.data.timetable
        - cronj.data.timer
        - cronj.core

#### v0.5.2

This version shifted various components around.

There is now a `cronj.global` to put the global timesheet and timeloop datastructures. Tasks are now triggered a watch function, as opposed to being triggered in the time-loop. I find this to be much more elegant and leads to looser coupling between the components, allowing for better component testing, which I still need to add.

I have moved `cronj.task`, `cronj.tab` and `cronj.timesheet` to  `cronj.data.task`, `cronj.data.tab` and `cronj.data.timesheet` respectively. `cronj.timekeeper` has been renamed to `cronj.loop`.

The `cronj.core` namespace has remained pretty constant. The `new-cronj!!` and `set-cronj!!` methods have been taken out, mainly to stop users (myself mainly) from shooting themselves in the foot by creating lots of cronj instances. Previously, there could be multiple cronj loops running with multiple timesheets. Now there now can only be one cronj time-loop initiated.
