(ns cronj.tab
  (:use [clojure.string :only [split]])
  (:require [clj-time.core :as t]))

;;(def !required-tab-keys [:id :tab-str :tab-arr])
(def SCHEDULE-ELEMENTS [:sec :minute :hour :day-of-week :day :month :year])

 ;; There are 2 different representations of cronj tab data:
 ;;   string: (for humans)        "   *       2,4      2-9         /8      ...  "
 ;;    array: (for efficiency)    [ (-*)   [2 4 6]   (-- 2 9)   (-- 8)     ...  ]
 ;;
 ;;            :tab-str                   :tab-arr
 ;;           +---------+                 +---------+
 ;;           |         |                 |         |
 ;;           |         |                 |         |
 ;;           | string  |    -----------> |  array  |
 ;;           |         |    parse-tab    |         |
 ;;           |         |                 |         |
 ;;           +---------+                 +---------+
 ;;            for human                    used in
 ;;            use to add                  cronj-loop
 ;;            tasks

;; Methods for type conversion
(defn- to-int [x] (Integer/parseInt x))

;; Array Representation
(defn- *-
  ([]      :*)
  ([s]     (fn [v] (zero? (mod v s))))
  ([a b]   (fn [v] (and (>= v a) (<= v b))))
  ([a b s] (fn [v] (and (>= v a)
                        (<= v b)
                        (zero? (mod (- v a) s))))))

;; String to Array Methods
(defn- parse-tab-elem [es]
  (cond (= es "*") :*
        (re-find #"^\d+$" es) (to-int es)
        (re-find #"^/\d+$" es) (*- (to-int (.substring es 1)))
        (re-find #"^\d+-\d+$" es)
        (apply *-
               (sort (map to-int (split es #"-"))))
        (re-find #"^\d+-\d+/\d$" es)
        (apply *-
               (map to-int (split es #"[-/]")))))

(defn- parse-tab-group [s]
  (let [e-toks (re-seq #"[^,]+" s)]
    (map parse-tab-elem e-toks)))

(defn parse-tab [s]
  (let [c-toks (re-seq #"[^\s]+" s)
        len-c (count c-toks)]
    (if (= (count SCHEDULE-ELEMENTS) len-c)
      (map parse-tab-group c-toks)
      (throw IllegalArgumentException "The schedule does not have the correct number of elements."))))

(defn valid-tab? [s]
  (try
    (parse-tab s)
    true
    (catch Exception e false)))


;; dt-arr methods
(defn to-dt-arr [dt]
  (map #(% dt)
       [t/sec t/minute t/hour t/day-of-week t/day t/month t/year]))

(defn truncate-ms [dt]
  (t/to-time-zone
   (apply t/date-time
          (map #(% dt)
               [t/year t/month t/day t/hour t/minute t/sec]))
   (t/default-time-zone)))

(defn- match-elem? [dt-e tab-e]
  (cond (= tab-e :*) true
        (= tab-e dt-e) true
        (fn? tab-e) (tab-e dt-e)
        (sequential? tab-e) (some #(match-elem? dt-e %) tab-e)
        :else false))

(defn match-arr? [dt-arr tab-arr]
  (every? true?
          (map match-elem? dt-arr tab-arr)))

;; task related methods
(defn assoc-tab [task s]
  {:pre [(valid-tab? s)]}
  (assoc task :tab-str s :tab-arr (parse-tab s)))

(defn tab-str [task]
  (:tab-str task))
