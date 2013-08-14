(defproject im.chit/cronj "0.8.1"
  :description "This is task-scheduling library inspired by the cron syntax."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-time "0.5.0"]
                 [im.chit/hara "1.0.1"]
                 [im.chit/ova "0.9.1"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]]}})
