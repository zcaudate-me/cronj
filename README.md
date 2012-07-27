# cronj

This is a cron-inspired task-scheduling library.

So the basic idea is the concept of "services" that have the following attributes:
      - "id" and "description" in order for normal people to understand what they do
      - "schedule", to specify the times that the service is required to run
      - "handler", the actual procedure that provides the functionality for the service

Services can be added and removed on the fly through the `cronj` library interface and `cronj` will keep an eye out on the time. Once a service has been scheduled to start, `cronj` will launch the handler in another thread.

I have found many scheduling libraries for clojure
  - [quartzite](https://github.com/michaelklishin/quartzite)
  - [cron4j](http://www.sauronsoftware.it/projects/cron4j)
  - [clj-cronlike](https://github.com/kognate/clj-cronlike)
  - [at-at](https://github.com/overtone/at-at)

However, none of them are suited to what I needed to do. The first three follow the cron convention. "Services" ("Jobs" or "Tasks") can only be scheduled at intervals of one minute apart.  The last scheduling library [at-at](https://github.com/overtone/at-at) had milli-second resolution, but was limited in the number of threads that was used. It was only good at looking after one service that did not overlap between calls.

I needed something that
  - started scheduled services with a per-second interval having high system-time accuracy.
  - would spawn as many threads as needed, so that services started at earlier intervals could exist along side services started at later intervals.
  - an additional design requirement required that service handlers are passed a date-time object, so that the service itself is aware of the time that it was started


## Usage
    (use '[cronj.core :only [-- -*]])
    (require '[cronj.core :as cj])
    (def every-five-seconds
      (cj/->Schedule. (-- 5)        ;; seconds in minute
                 (-*)          ;; minutes in hour
                 (-*)          ;; hours in day
                 (-*)          ;; day of week
                 (-*)          ;; day of month
                 (-*)          ;; month of year
                 (-*)          ;; year
      ))

    (cj/add-service {:id 0 :desc 0 :handler #(println "job 0:" %) :schedule every-five-seconds})
    (cj/add-service {:id 1 :desc 1 :handler #(println "job 1:" %) :schedule "/3 * * * * * *"}) ;; every 3 seconds
    (cj/start!) ;; default checking interval is 50ms, try (cj/start! 20) for a checking interval of 20ms
    ;; to stop, type (cj/stop!)


#### More cron-like usage

    (cj/add-service {:id "print-date"
                 :desc "prints out the date every 5 seconds"
                 :handler #'println
                 :schedule "/5 * * * * * *"})

    (cj/add-service {:id "print-date"
                 :desc "prints out the date every 5 seconds on the
                        9th and 10th minute of every hour on every Friday
                        from June to August between the year 2012 to 2020"
                 :handler #'println
                 :schedule "/5  9,10  * 5 * 6-8 2012-2020"})

## Todo:
#### commandline usage for single shell programs

    java -jar cronj.jar --service "echo $(date)" --schedule "/5 * * * * * *"

## License
Copyright © 2012 Chris Zheng

Distributed under the Eclipse Public License, the same as Clojure.
