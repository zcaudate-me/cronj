(defproject im.chit/cronj "1.0.1"
  :description "A simple to use, cron-inspiried task scheduler"
  :url "http://github.com/zcaudate/cronj"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-time "0.6.0"]
                 [im.chit/hara "1.0.1"]
                 [im.chit/ova "1.0.1"]]
  :profiles {:dev {:dependencies [[midje "1.6.0"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :documentation {:files {"docs/index"
                          {:input "test/midje_doc/cronj_guide.clj"
                           :title "cronj"
                           :sub-title "task scheduling and simulation"
                           :author "Chris Zheng"
                           :email  "z@caudate.me"
                           :tracking "UA-31320512-2"}}})
