(defproject im.chit/cronj "1.4.1"
  :description "A simple to use, cron-inspiried task scheduler"
  :url "http://github.com/zcaudate/cronj"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [clj-time "0.8.0"]
                 [im.chit/hara.common.primitives "2.1.3"]
                 [im.chit/hara.namespace.import "2.1.3"]
                 [im.chit/hara.ova "2.1.3"]
                 [im.chit/hara.concurrent.latch "2.1.3"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-midje-doc "0.0.24"]]}}
  :documentation {:files {"docs/index"
                          {:input "test/midje_doc/cronj_guide.clj"
                           :title "cronj"
                           :sub-title "task scheduling and simulation"
                           :author "Chris Zheng"
                           :email  "z@caudate.me"
                           :tracking "UA-31320512-2"}}})
