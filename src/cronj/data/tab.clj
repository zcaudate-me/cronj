(ns cronj.data.tab
  (:require [clojure.string :refer [split]]
            [hara.common.error :refer [error suppress]]
            [hara.common.fn :refer [F]]
            [clj-time.core :as t]
            [clj-time.local :as lt]))

(def SCHEDULE-ELEMENTS [:second :minute :hour :day-of-week :day :month :year])

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
               (map to-int (split es #"[-/]")))
        :else (error es " is not in the right format.")))

(defn- parse-tab-group [s]
  (let [e-toks (re-seq #"[^,]+" s)]
    (map parse-tab-elem e-toks)))

(defn parse-tab [s]
  (let [c-toks (re-seq #"[^\s]+" s)
        len-c (count c-toks)
        sch-c  (count SCHEDULE-ELEMENTS)]
    (cond (= sch-c len-c) (map parse-tab-group c-toks)
          (= (dec sch-c) len-c) (map parse-tab-group (cons "0" c-toks))
          :else
          (error "The schedule " s
                 " does not have the correct number of elements."))))

(defn valid-tab? [s]
  (suppress (if (parse-tab s) true)))

;; dt-arr methods
(defn to-dt-array [dt]
  (map #(% dt)
       [t/second t/minute t/hour t/day-of-week t/day t/month t/year]))

(defn truncate-ms [dt]
  (lt/to-local-date-time
   (apply t/date-time
          (map #(% dt)
               [t/year t/month t/day t/hour t/minute t/second]))))

(defn- match-elem? [dt-e tab-e]
  (cond (= tab-e :*) true
        (= tab-e dt-e) true
        (fn? tab-e) (tab-e dt-e)
        (sequential? tab-e) (some #(match-elem? dt-e %) tab-e)
        :else false))

(defn match-array? [dt-array tab-array]
  (every? true?
          (map match-elem? dt-array tab-array)))

(def nil-array [F F F F F F F])
