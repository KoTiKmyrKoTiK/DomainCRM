(ns common.utils.date.re
  "Regexps used in date utils."
  (:require [clojure.string :as str])
  (:refer-clojure :exclude [time min]))

;; Helpers
;; ----------

(defn- re->str
  ^String [re]
  #?(:clj  (. re pattern)
     :cljs (. re -source)))

(defn re-conj
  "Joins multiple regexps"
  [regexps]
  (letfn [(str
            [s]
            (if-not (string? s)
              (re->str s)
              s))]
    (->> regexps
         (map str)
         (str/join "")
         re-pattern)))

(defn agg-fmts
  "Returns regexps that matches any of the given fmts."
  [& formats]
  (->> formats
       (map #(str "(" (re->str %) ")"))
       (str/join "|")
       re-pattern))

(defn opt
  "Optional part"
  [parts]
  (re-conj (concat ["("] parts [")?"])))

(defn re-pattern?
  [x]
  (= (type #"") (type x)))

;; Validators
;; ----------

;; `Lax` means that year-with-month and year are considered valid formats.
;; `Strict` ignores them.

(def year    #"(\d{4})")
(def month   #"((?:0[1-9])|(?:1[012])|(?:2[123]))")
(def day     #"(0[1-9]|[12][0-9]|3[01])")

(def hour    #"((?:[2][0-3])|(?:[0-1]?[0-9]))")
(def min     #"([0-5][0-9])")
(def sec     #"([0-5][0-9])")
(def msec    #"\d+")

(def tz-sign #"((\+(?=\d))|(\-(?=\d))|(Z(?!.)))")

(def tz                    (re-conj [tz-sign (opt [hour (opt [":?" min])])]))
(def wrong-tz              (re-conj [tz-sign (opt [hour (opt [min])])]))
(def time                  (re-conj [hour ":" min (opt [":" sec (opt ["\\." msec]) (opt [tz])])]))
(def time-no-sec           (re-conj [hour ":" min (opt [(opt [":" sec]) (opt ["\\." msec]) (opt [tz])])]))

(def iso-datetime          (re-conj [year "-" month "-" day "T" time]))
(def iso-datetime-tz       (re-conj [year "-" month "-" day "T" time tz]))
(def iso-datetime-wrong-tz (re-conj [year "-" month "-" day "T" time wrong-tz]))
(def iso-datetime-lax      (re-conj [year "-" month "-" day "T" time-no-sec]))
(def iso-date-strict       (re-conj [year "-" month "-" day "(T" time ")?"]))
(def iso-date-lax          (re-conj [year "(" "-" month ")?(" "-" day ")?"]))

(def ru-datetime           (re-conj [day "\\." month "\\." year " " time]))
(def ru-date-strict        (re-conj [day "\\." month "\\." year]))
(def ru-date-lax           (re-conj ["(" day "\\." ")?(" month "\\." ")?" year]))
(def ru-month-and-day      (re-conj [day "\\." month]))

(type ru-date-lax)

(def validators
  (->> (ns-interns 'common.utils.date.re)
       (reduce-kv (fn [acc sym var]
                    (let [val (deref var)]
                      (cond-> acc
                        (re-pattern? val)
                        (assoc sym val))))
                  {})))
