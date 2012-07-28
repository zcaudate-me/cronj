## cronj

This is a cron-inspired task-scheduling library.

### Installation:
 
In project.clj, add to dependencies:
     
     [cronj "0.1.0"]

### Idea

So the basic idea is the concept of a "task" that have the following attributes:

      - "id" and "description" for meta description
      - "schedule", to specify when the task should run
      - "handler", the actual procedure that provides the functionality for a task

Tasks can be added and removed on the fly through the `cronj` library interface and `cronj` will keep an eye out on the time. At the time a task has been scheduled to start, `cronj` will launch the task handler in another thread.

### Motivation

I have found many scheduling libraries for clojure
  - [quartzite](https://github.com/michaelklishin/quartzite)
  - [cron4j](http://www.sauronsoftware.it/projects/cron4j)
  - [clj-cronlike](https://github.com/kognate/clj-cronlike)
  - [at-at](https://github.com/overtone/at-at)
  - [monotony](https://github.com/aredington/monotony)

However, none of them are suited to what I needed to do. The first three all follow the cron convention. The "task" (also called a "job") can only be scheduled at whole minute intervals. [at-at](https://github.com/overtone/at-at) has milli-second resolution, but was limited in the number of threads that was predetermined. It was good for looking after tasks that did not overlap between calls but not for tasks that may take an arbitarily long time. [monotony](https://github.com/aredington/monotony) uses core.logic, which is something that I am yet to understand.

I needed something that
  - started scheduled tasks with a per-second interval having high system-time accuracy.
  - would spawn as many threads as needed, so that tasks started at earlier intervals could exist along side tasks started at later intervals.
  - an additional design requirement required that task handlers are passed a date-time object, so that the handler itself is aware of the time when it was initiated.

## Usage:
    (require '[cronj.core :as cj])

    (cj/add-task {:id 0   :desc 0 
                  :handler #(println "job 0:" %) 
                  :schedule "/5 * * * * * *"}) ;; every 5 seconds
    (cj/add-task {:id 1   :desc 1 
                  :handler #(println "job 1:" %) 
                  :schedule "/3 * * * * * *"}) ;; every 3 seconds

    (cj/start!) ;; default interval to check the current time is 50ms, 
                ;; try (cj/start! 20) for an interval of 20ms

    ;; wait for outputs ......

    ;; get bored
    
    (cj/stop!)


##### tutorial of task scheduling control

tasks can be added and removed whilst cronj is running:

    (cj/start!)
    
    ;; cronj is running 

    (cj/add-task {:id 0   :desc 0 
                  :handler #(println "job 0:" %) 
                  :schedule "/5 * * * * * *"}) ;; every 5 seconds
    
    ;; wait a bit and add another task
    
    (cj/add-task {:id 1   :desc 1 
        :handler #(println "job 1:" %) 
        :schedule "/3 * * * * * *"}) ;; every 3 seconds    

    ;; actually, this task is useless
    
    (cj/remove-task 1)
    
tasks can be individually enabled and disabled:

    (cj/disable-task 0)   ;;=> disables the task with :id 0
    (cj/enable-task 0)    ;;=> enables the task again
    (cj/toggle-task 0)    ;;=> toggles the task
    
similarily, operations can be done on the entire list:

    (cj/enable-all-tasks)  
    (cj/disable-all-tasks)   ;; you should get the idea
    (cj/toggle-all-tasks)

task schedules can also be updated dynamically:

    ;; whilst cronj is running
    (cj/set-schedule 0 "/2 * * * * * *") ;; every 2 seconds instead of 3

task handlers can also be updated:

    (cj/set-handler 0 #(println "job 0 changed handlers:" %))

##### More cron-like usage:

    (cj/add-task {:id "print-date"
                 :desc "prints out the date every 5 seconds"
                 :handler #'println
                 :schedule "/5 * * * * * *"})

    (cj/add-task {:id "print-date"
                 :desc "prints out the date every 5 seconds on the
                        9th and 10th minute of every hour on every Friday
                        from June to August between the year 2012 to 2020"
                 :handler #'println
                 :schedule "/5  9,10  * 5 * 6-8 2012-2020"})

##### launching shell commands

Shell commands can be launched using the alternative map description:

    (cj/add-task {:id "print-date-sh"
                 :desc "prints out the date every 5 seconds from the shell's 'date' function"
                 :cmd "date"
                 :schedule "/5 * * * * * *"})

An output function can be added, which will be passed a `datetime` value and the output map of the finished
shell command. See source code of `sh-print` for the simplest example usage.

    (cj/add-task {:id "print-date-sh-2"
                 :desc "prints out the date every 5 seconds from the shell's 'date' function"
                 :cmd "date"
                 :cmd-fn (fn [dtime output] (println (format "At %s, command 'date' output the value %s"
                                                             (str dtime)
                                                             (:out output))))
                 :schedule "/5 * * * * * *"})


## Todo:
##### commandline usage for single shell programs

    java -jar cronj.jar --task "echo $(date)" --schedule "/5 * * * * * *"

## License
Copyright © 2012 Chris Zheng

Distributed under the Eclipse Public License, the same as Clojure.
