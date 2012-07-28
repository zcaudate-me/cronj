(defproject cronj "1.0.0-SNAPSHOT"
  :description "This is task-scheduling library inspired by the cron syntax."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-time "0.4.3"]
                 [midje "1.4.0"]
                 [com.stuartsierra/lazytest "1.2.3"]]
   :repositories {"stuart" "http://stuartsierra.com/maven2"})