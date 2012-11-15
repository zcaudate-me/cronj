(ns cronj.test-tab
    (:use midje.sweet)
    (:require [hara.dyna :as d]
              [clj-time.core :as t]
              [cronj.data.tab :as tb] :reload))

(def test-num (range 60))
(def -* #'tb/*-)
(def every-5-seconds-0 [(-* 0 60 5) (-*) (-*) (-*) (-*) (-*) (-*)])
(def every-5-seconds-1 [(-* 5) (-*) (-*) (-*) (-*) (-*) (-*)])

;; Unit Tests
(fact "*- takes a string and returns something"
  (#'tb/*-) => :*
  (map (#'tb/*- 2) test-num) => (map even? test-num)
  (map (#'tb/*- 0 10) test-num) => (map (fn [x] (and (>= x 0) (<= x 10))) test-num))

(tabular
 (fact "Testing the 'to-time-array' function"
   (tb/to-dt-arr ?time) => ?expected)

 ?time               ?expected
 (t/epoch)           [0 0 0 4 1 1 1970]
 (t/date-time 2000)  [0 0 0 6 1 1 2000])

(fact "parse-str takes a string and creates matches"
  (tb/parse-tab "* * * * * * *") => '[(:*) (:*) (:*) (:*) (:*) (:*) (:*)]
  (tb/parse-tab "1,2 1,5 * * 1 * *") => '[(1 2) (1 5) (:*) (:*) (1) (:*) (:*)])

(tabular
 (fact "Testing the 'match-entry?' function"
   (#'tb/match-elem? ?t-entry ?c-entry) => ?expected)
 ?t-entry        ?c-entry           ?expected
 1               :*                 true
 1               [:*]               true
 1               [2 3 4]            falsey
 1               [1 2 3]            true
 1               [2 3 4 :*]         true
 1               (-*)               true
 1               [(-*)]             true
 30              (-* 30 40)         true
 29              (-* 30 40)         falsey
 40              (-* 30 40)         true
 30              (-* 30 40 5)       true
 40              (-* 30 40 5)       true
 35              (-* 30 40 5)       true
 36              (-* 30 40 5)       falsey
 30              (-* 5)             true
 30              (-* 2)             true
 35              (-* 5)             true
 35              (-* 2)             falsey)

(tabular
 (fact "Testing the 'match-arr?' function"
   (tb/match-arr? ?t-arr ?c-arr) => ?expected)

 ?t-arr                    ?c-arr            ?expected
 [30 14 0 4 26 7 2012]     every-5-seconds-0   true
 [31 14 0 4 26 7 2012]     every-5-seconds-0   false
 [30 14 0 4 26 7 2012]     every-5-seconds-1   true
 [31 14 0 4 26 7 2012]     every-5-seconds-1   false)

(tb/match-arr? [30 14 0 4 26 7 2012]  every-5-seconds-0)

(fact "assoc-tab and tab-str"
  (tb/tab-str (tb/assoc-tab {} "* * * * * * *")) => "* * * * * * *")
