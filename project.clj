(defproject cronj "0.7.0"
  :description "This is task-scheduling library inspired by the cron syntax."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-time "0.5.0"]
                 [hara "0.7.0"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]]}})
