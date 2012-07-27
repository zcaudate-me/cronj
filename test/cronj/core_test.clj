(ns cronj.core-test
  (:use midje.sweet
        [cronj.core :only [-- -*]])
  (:require [cronj.core :as cj]
            [clj-time.core :as t]))

(tabular
 (fact "Testing the 'to-time-array' function"
   (#'cj/to-time-array ?time) => ?expected)

 ?time               ?expected
 (t/epoch)           '(0 0 0 4 1 1 1970))

(tabular
 (fact "Testing the 'match-entry?' function"
   (#'cj/match-array-entry? ?t-entry ?c-entry) => ?expected)

 ?t-entry        ?c-entry           ?expected
 1               :*                 true
 1               [:*]               true
 1               [2 3 4]            falsey
 1               [1 2 3]            true
 1               [2 3 4 :*]         true
 1               (-*)               true
 1               [(-*)]             true
 30              (-- 30 40)         true
 29              (-- 30 40)         falsey
 40              (-- 30 40)         true
 30              (-- 30 40 5)       true
 40              (-- 30 40 5)       true
 35              (-- 30 40 5)       true
 36              (-- 30 40 5)       falsey
 30              (-- 5)             true
 30              (-- 2)             true
 35              (-- 5)             true
 35              (-- 2)             falsey)

(def every-5-seconds-0 [(-- 0 60 5) (-*) (-*) (-*) (-*) (-*) (-*)])
(def every-5-seconds-1 [(-- 5) (-*) (-*) (-*) (-*) (-*) (-*)])

(tabular
 (fact "Testing the 'match-cron?' function"
   (#'cj/match-array? ?t-arr ?c-arr) => ?expected)

 ?t-arr                    ?c-arr            ?expected
 [30 14 0 4 26 7 2012]     every-5-seconds-0   true
 [31 14 0 4 26 7 2012]     every-5-seconds-0   false
 [30 14 0 4 26 7 2012]     every-5-seconds-1   true
 [31 14 0 4 26 7 2012]     every-5-seconds-1   false)
