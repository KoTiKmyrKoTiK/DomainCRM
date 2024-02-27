(ns common.utils.date.utils
  "Additional, more specific utils."
  (:require [clojure.string :as str]
            [common.utils.date :as dh]
            #?@(:clj [[java-time :as jt]]))
  #?(:clj (:import (java.time.temporal WeekFields))))

(defn month-before-now
  "Returns date that is a month before local now as datetime obj."
  []
  (dh/minus (dh/now) {:month 1}))

(defn regress-search
  "Tries to get date string from the search and returns it into the equivalent ISO-fmt.
   
   Returns `nil` if no formats are found."
  [search]
  (cond
    (dh/any-fmt? search)
    (dh/date->iso search)

    (str/blank? search)
    nil

    :else
    (recur (subs search 0 (dec (count search))))))

(defn convert-time
  "Reduces datetime object to a single value. Can only go to a lesser unit.
   
   Can parse strings."
  [datetime to]
  (let [rule {:hour [:min 60]
              :min  [:sec 60]}]
    (loop [result (dh/parse datetime)]
      (if (= #{to} (set (keys result)))
        (get result to)
        (recur
         (reduce-kv
          (fn [acc k v]
            (if (= k to)
              (reduced (update acc k (fnil + 0) v))
              (let [[k' v'] (get rule k)]
                (assoc acc k' (* v' v)))))
          {} result))))))

(defn current-week-number
  "Returns week number for current date in a PostgreSQL way."
  []
  #?(:clj  (let [year              (jt/year)
                 current-date      (jt/local-date-time)
                 first-day-of-year (. year atDay 1)
                 first-day-of-week (. first-day-of-year getDayOfWeek)]
             (->> (WeekFields/of first-day-of-week 1)
                  (.weekOfWeekBasedYear)
                  (. current-date get)))
     :cljs (let [current-date (js/Date.)
                 first-day-of-year (js/Date.
                                    (str (. current-date getFullYear) "-01-01"))]
             (-> (- current-date first-day-of-year)
                 (quot (* 24 60 60 1000))
                 (quot 7)
                 (+ 1)))))
