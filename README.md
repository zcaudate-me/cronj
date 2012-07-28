# cronj

This is a cron-inspired task-scheduling library.

So the basic idea is the concept of a "task" that have the following attributes:

      - "id" and "description" for meta description
      - "schedule", to specify when the task should run
      - "handler", the actual procedure that provides the functionality for a task

Tasks can be added and removed on the fly through the `cronj` library interface and `cronj` will keep an eye out on the time. Once a task has been scheduled to start, `cronj` will launch the task-handler in another thread.

I have found many scheduling libraries for clojure
  - [quartzite](https://github.com/michaelklishin/quartzite)
  - [cron4j](http://www.sauronsoftware.it/projects/cron4j)
  - [clj-cronlike](https://github.com/kognate/clj-cronlike)
  - [at-at](https://github.com/overtone/at-at)

However, none of them are suited to what I needed to do. The first three all follow the cron convention. The "task" (also called a "job") can only be scheduled at whole minute intervals. The last scheduling library [at-at](https://github.com/overtone/at-at) had milli-second resolution, but was limited in the number of threads that was used. It was only good for looking after a single task that did not overlap between calls.

I needed something that
  - started scheduled tasks with a per-second interval having high system-time accuracy.
  - would spawn as many threads as needed, so that tasks started at earlier intervals could exist along side tasks started at later intervals.
  - an additional design requirement required that task handlers are passed a date-time object, so that the handler itself is aware of the time when it was initiated.

## Usage
    (require '[cronj.core :as cj])
    (cj/add-task {:id 0 :desc 0 :handler #(println "job 0:" %) :schedule "/5 * * * * * *"}) ;; every 5 seconds
    (cj/add-task {:id 1 :desc 1 :handler #(println "job 1:" %) :schedule "/3 * * * * * *"}) ;; every 3 seconds
    (cj/start!) ;; default checking interval is 50ms, try (cj/start! 20) for a checking interval of 20ms
    ;; to stop, type (cj/stop!)
    (cj/stop!)


#### More cron-like usage

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

#### launching shell commands

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
#### commandline usage for single shell programs

    java -jar cronj.jar --task "echo $(date)" --schedule "/5 * * * * * *"

## License
Copyright © 2012 Chris Zheng

Distributed under the Eclipse Public License, the same as Clojure.
