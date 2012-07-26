# cronj

This is a cron-inspired task-scheduling library. I have found many cron-like libraries not quite usable in that tasks can only be scheduled at one minute intervals. I want to be able to have more precision in how my tasks are run (so that a task should start as close to the second as possible). Also, many tasks require the datestamp be passed in, so all jobs require a handler that takes in a datetime object.

## Usage
    (use '[cronj.core :only [-- -* -|]])
    (require '[cronj.core :as cj])
    (def every-five-seconds
      [ (-| 5)   ;; seconds in minute 
        (-*)          ;; minutes in hour
        (-*)          ;; hours in day
        (-*)          ;; day of week
        (-*)          ;; day of month
        (-*)          ;; month of year
        (-*)          ;; year
      ])
    (cj/add-job {:id 0 :desc 0 :handler #'println :schedule every-five-seconds})
    (cj/start!) ;; default checking interval is 100ms, try (cj/start! 20) for a checking interval of 20ms
    (cj/stop!)

## TODOS
####string parsing for more cron-like usage, ie: 

    (cj/add-job {:id "print-date" 
                 :desc "prints out the date every 5 seconds"  
                 :handler #'println 
                 :schedule "/5 * * * * * *"})

    (cj/add-job {:id "print-date" 
                 :desc "prints out the date every 5 seconds on the
                        9th and 10th minute of every hour on every Friday
                        from June to August between the year 2012 to 2020"  
                 :handler #'println 
                 :schedule "/5  9,10  * 5 * 6-8 2012-2020"})

####commandline usage for single shell programs

    java -jar cronj.jar --job "echo $(date)" --schedule "/5 * * * * * *"

## License
Copyright © 2012 Chris Zheng

Distributed under the Eclipse Public License, the same as Clojure.
