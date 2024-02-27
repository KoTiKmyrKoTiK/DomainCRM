(ns common.utils.date.pre
  "Pre`s for functions workings with dates."
  (:require [common.utils.date.re :as re]))

;; `Lax` means that year-with-month and year are considered valid formats.
;; `Strict` ignores them.

(defn ^::lax iso-fmt?
  "Format is YYYY-MM-DDThh:mm:ss+hh:mm.
   
   Lax."
  [^String s]
  (when (string? s)
    (boolean
     (re-matches
      (re/agg-fmts re/iso-datetime
                   re/iso-date-lax)
      s))))

(defn ^::lax is-datetime-tz?
  "Format is YYYY-MM-DDThh:mm:ss+hh:mm.
   
   Lax."
  [^String s]
  (when (string? s)
    (boolean
     (re-matches
      (re/agg-fmts re/iso-datetime-tz)
      s))))

(defn iso-fmt-without-second?
  "Format is YYYY-MM-DDThh:mm+hh:mm."
  [^String s]
  (when (string? s)
    (boolean
     (re-matches re/iso-datetime-lax s))))

(defn ^::lax ru-fmt?
  "Format is DD.MM.YYYY hh:mm:ss.
   
   Lax."
  [^String s]
  (when (string? s)
    (boolean
     (re-matches
      (re/agg-fmts re/ru-datetime
                   re/ru-date-lax
                   re/ru-month-and-day)
      s))))

(defn ^::lax all-fmt?
  "Virtual format that corresponds with the family `:all`.
   
   Lax."
  [^String s]
  (when (string? s)
    (boolean
     (re-matches
      (re/agg-fmts re/year
                   re/time)
      s))))

(defn ^::lax ru-date?
  "Format is DD.MM.YYYY
   
   Lax."
  ;; Used in _ilike cond search by date.
  [^String s]
  (when (string? s)
    (boolean
     (re-matches
      (re/agg-fmts re/ru-date-lax)
      s))))

(defn ^::lax any-fmt?
  "Format is \"iso\" or \"ru\".
   
  Lax.
   
   Lookup `iso-fmt?`, `ru-fmt?`."
  [^String s]
  (when (string? s)
    ((some-fn all-fmt? iso-fmt? ru-fmt?) s)))

(defn ^::lax is-datetime?
  "Checks that `s` is a datetime object or a string that is parsable to a datetime object.
   
   Lax.
   
   Lookup `datetime`."
  [s]
  (boolean
   ((cond
      (string? s)
      any-fmt?

      (map? s)
      (every-pred :year :month :day)

      :else
      (constantly nil))
    s)))

(defn ^::strict date?
  "Checks that `s` is in iso-date or ru-date fmt.
   
   Strict."
  [^String s]
  (when (string? s)
    (boolean
     ((some-fn (partial re-matches re/iso-date-strict)
               (partial re-matches re/ru-date-strict))
      s))))

(defn ^::strict wrong-tz-format?
  "When format of tz without semicolon"
  [^String s]
  (when (string? s)
    (boolean
     (re-matches
      (re/agg-fmts re/iso-datetime-wrong-tz)
      s))))
