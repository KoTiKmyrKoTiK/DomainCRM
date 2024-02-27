(ns common.utils.date
  "Utils to work with date. All functions return `nil` in case of invalid operations.
   
   Call `formats` to get all currently available formats."
  {:clj-kondo/config '{:linters {:unused-private-var {:level :off}}}}
  (:require [clojure.string                 :as str]
            [common.utils.core              :as core]
            [common.utils.macro             :refer [copy-def]]
            [common.utils.date.macro        :refer [def-format]]
            [common.utils.date.re           :as re]
            [common.utils.date.pre          :as pre]
            [unit-map.ops                   :as u.ops]
            [unit-map.type.chrono.datetime :as  u.datetime]
            #?@(:clj [[java-time :as jt]]
                :cljs [[cljsjs.moment]
                       [cljsjs.moment-timezone]])))

;; Init
;; ------

(defmethod u.ops/definition :default-type [_] u.datetime/gregorian-military)

;; ------

(defrecord ^{:private true
             :doc "A example of datetime object.\n\nFor demonstration purposes only. Do not use."}
 datetime
  [year month day hour min sec tz])

(declare now utc-now tz->utc tz->utc tz->local date->iso)
(declare formats parse)
(declare iso-fmt? ru-fmt? ru-fmt? date?)
(declare compare-date lt? gt? lte? gte?)
(declare iso-datetime-ms-with-tz iso-datetime-ms-without-sec-with-tz iso-datetime-with-tz 
         iso-datetime iso-datetime-short iso-date ru-datetime-short ru-datetime ru-date-and-time
         ru-date ru-datetime-human time-short time-std time-full)

(def max-date
  #?(:clj  "9999-12-31"
     :cljs "2106-12-04"))

;; Formats
;; --------------------------------------------

;; Inner

(def ^:private escape-chars
  {\+ "\\+"
   \. "\\."})

(declare defaults)
(declare part-length)
(declare ru-month-name)
(declare parse*)

(defn- as-datepart
  "Returns val formatted as datepart"
  [part val]
  (cond
    (map? part)
    (let [[_ func] (first part)]
      (func val))

    (= val "Z")
    val

    (not (or (nil? val) (#{0 "0"} val)))
    (if (> (part-length part) (count (str val)))
      (if (= part :msec)
        (cond
          (< val 10)  (str "00" val)
          (< val 100) (str "0" val)
          :else      val)
        (str (apply str (map (fn [_] 0) (range (- (part-length part) (count (str val)))))) val))
      (str val))

    :else
    (defaults part)))

(defn- format-date*
  ([obj fmt]
   (format-date* "" obj fmt))
  ([s obj fmt]
   (when (map? obj)
     (if (seq fmt)
       (let [[key-arg sep & [next-key & _ :as rest-arg]] fmt]
         (recur (str s
                     (let [val (if (map? key-arg)
                                 (get obj (ffirst key-arg))
                                 (get obj key-arg))]
                       (as-datepart key-arg val))
                     (if (sequential? sep) ; Sep should be in the val then, like in :tz.
                       (when-not (when-let [next-val (next-key obj)]
                                   (->> ["+" "-" "Z"]
                                        (map #(str/escape % escape-chars))
                                        (str/join "|")
                                        re-pattern
                                        (#(re-find % next-val))))
                         "")
                       sep))
                obj rest-arg))
       s))))

(defn format-date
  [s fmt]
  (cond-> s
    (string? s)
    parse

    :always
    (format-date* fmt)))

;; Formats

(def-format ^:iso iso-datetime-ms-with-tz
  "YYYY-MM-DDThh:mm:ss.msec+hh:mm"
  [:year "-" :month "-" :day "T" :hour ":" :min ":" :sec "." :msec ["+" "-" "Z"] :tz])

(def-format ^:iso iso-datetime-ms-without-sec-with-tz
  "YYYY-MM-DDThh:mm:ss.msec+hh:mm"
  [:year "-" :month "-" :day "T" :hour ":" :min ["+" "-" "Z"] :tz])

(def-format ^:iso iso-datetime-with-tz
  "YYYY-MM-DDThh:mm:ss+hh:mm"
  [:year "-" :month "-" :day "T" :hour ":" :min ":" :sec ["+" "-" "Z"] :tz])

(def-format ^:iso iso-datetime
  "YYYY-MM-DDThh:mm:ss"
  [:year "-" :month "-" :day "T" :hour ":" :min ":" :sec])

(def-format ^:iso iso-datetime-short
  "YYYY-MM-DDThh:mm"
  [:year "-" :month "-" :day "T" :hour ":" :min])

(def-format ^:iso iso-date
  "YYYY-MM-DD"
  [:year "-" :month "-" :day])

(def-format ^:ru ru-datetime-short
  "DD.MM.YYYY hh:mm"
  [:day "." :month  "." :year " " :hour ":" :min])

(def-format ^:ru ru-datetime
  "DD.MM.YYYY hh:mm:ss"
  [:day "." :month  "." :year " " :hour ":" :min ":" :sec])

(def-format ^:ru ru-date-and-time
  "DD.MM.YYYY - hh:mm"
  [:day "." :month  "." :year " - " :hour ":" :min])

(def-format ^:ru ru-date
  "DD.MM.YYYY"
  [:day "." :month  "." :year])

(def-format ^:ru ru-date-human
  "DD [А-Яа-я]* YYYY"
  [:day " " {:month ru-month-name}  " " :year])

(def-format ^:ru ru-datetime-human
  "DD [А-Яа-я]* YYYY hh:mm"
  [:day " " {:month ru-month-name}  " " :year " " :hour ":" :min])

(def-format ^:all time-short
  "hh:mm"
  [:hour ":" :min])

(def-format ^:all time-std
  "hh:mm:ss"
  [:hour ":" :min ":" :sec])

(def-format ^:all time-full
  "hh:mm:ss.msec"
  [:hour ":" :min ":" :sec "." :msec])

(def formats
  "All available date formats."
  (->> (ns-interns 'common.utils.date)
       (keep (fn [[sym var]]
               (when (and (::format (meta var))
                          (not (:private (meta var))))
                 {(with-meta sym (meta var)) (:doc (meta var))})))
       (apply merge)))

;; Inner formats
;; ------------------------------

;; ISO

(def-format ^:private ^:iso iso-fmtZ
  "YYYY-MM-DDThh:mm:ssZ"
  [:year "-" :month "-" :day "T" :hour ":" :min ":" :sec ["+" "-" "Z"] :tz])

(def-format ^:private ^:iso iso-year-and-month
  "YYYY-MM"
  [:year "-" :month])

(def-format ^:private ^:iso iso-month-and-day
  "MM-DD"
  [:month "-" :day])

;; RU

(def-format ^:private ^:ru ru-fmt
  "DD.MM.YYYY hh:mm:ss"
  [:day ".(?=(\\d{2}.))" :month  "." :year " " :hour ":" :min ":" :sec])

(def-format ^:private ^:ru ru-year-and-month
  "MM.YYYY"
  [:month "." :year])

(def-format ^:private ^:ru ru-month-and-day
  "DD.MM"
  [:day "." :month])

;; All

(def-format ^:private ^:all year
  "YYYY"
  [:year])

(def-format ^:private ^:all time-only
  "hh:mm:ss"
  [:hour ":" :min ":" :sec])

;; --------

(def ^:private formats*
  "Variant of `format` that works with private formats."
  (->> (ns-interns 'common.utils.date)
       (keep (fn [[sym var]]
               (when (::format (meta var))
                 {(with-meta sym (meta var)) (:doc (meta var))})))
       (apply merge)))

(defonce app-tz
  #?(:clj (try
            (let [zone-id
                  (or
                   (some-> "Europe/Moscow"
                           (jt/system-clock)
                           (jt/with-clock (jt/zone-id))) ; For production use
                   (jt/zone-id))                         ; For local use
                  ]
              (-> (java.time.Instant/now)
                  (jt/to-java-date)
                  (jt/offset-date-time zone-id)
                  (.getOffset)
                  str))
            (catch Exception e (do (prn e)
                                   (throw e))))
     :cljs (let [zone (. (. js/moment -tz) guess)
                 date (. (. js/moment tz (js/Date.) zone) format)]
             (->> date (re-find #"(?:Z|[+-](?:2[0-3]|[01][0-9]):[0-5][0-9])")))))

;; Defaults
;; --------------------------
(defn defaults
  [key]
  (case key
    :year             "0001"
    (:month :day)     "01"
    (:hour :min :sec) "00"
    :msec             "000"
    :tz               app-tz
    nil))

(defn- part-length
  [key]
  (count (defaults key)))

;; Format checkers
;; ----------------------------------------

(copy-def iso-fmt?         pre/iso-fmt?)
(copy-def iso-fmt-lax?     pre/iso-fmt-without-second?)
(copy-def ru-fmt?          pre/ru-fmt?)
(copy-def all-fmt?         pre/all-fmt?)
(copy-def ru-date?         pre/ru-date?)
(copy-def any-fmt?         pre/any-fmt?)
(copy-def is-datetime?     pre/is-datetime?)
(copy-def is-datetime-tz?  pre/is-datetime-tz?)
(copy-def date?            pre/date?)
(copy-def wrong-tz-format? pre/wrong-tz-format?)

(defn- get-fmt
  "Returns symbol that correlates with fmt of given string."
  [s]
  (when (any-fmt? s)
    (let [family (cond
                   (all-fmt? s) :all
                   (iso-fmt? s) :iso
                   (ru-fmt? s)  :ru)]
      (->> formats*
           (filter (fn [[sym _]]
                     ((some-fn family :all) (meta sym))))
           (map (fn [[sym fmt]]
                  [sym (re-pattern (-> fmt
                                       (str/replace #"[YMDhmsec]" #?(:clj  "\\\\d"
                                                                     :cljs "\\d"))
                                       (str/escape {\* "+"
                                                    \. "\\."
                                                    \+ "(\\+|\\-|Z)"})))]))
           (filter (fn [[_ regexp]]
                     (re-matches regexp s)))
           (ffirst)))))

(defn- get-fmt-fn
  "Returns function that correlates with fmt of given string."
  [s]
  (let [fmt (get-fmt s)]
    (get (ns-interns 'common.utils.date) fmt)))

(defn is-fmt?
  "Checks that date corresponds to the fmt"
  [fmt ^String s]
  (= (fmt s) s))

;; Parser
;; -----------------------------------------

(defn- parse*
  ([s fmt]
   (parse* {} s fmt))
  ([obj s fmt]
   (if (and (not (str/blank? s))
            (seq fmt))
     (let [[key pattern* & fmt-rest] fmt]
       (if pattern*
         (let [pattern        (re-pattern
                               (cond-> pattern*
                                 (sequential? pattern*)
                                 ((partial str/join "|"))

                                 :always
                                 (str/escape escape-chars)))
               pattern'       (when-let [pattern* (re-find pattern s)]
                                (if (sequential? pattern*)
                                  (first pattern*) pattern*))
               [val & s-rest] (if pattern'
                                (str/split s (-> pattern'
                                                 (str/escape escape-chars)
                                                 re-pattern))
                                [nil s])
               fmt-rest'      (if pattern'
                                (vec fmt-rest)
                                (assoc (vec fmt-rest) 0 key))]
           (recur (if val
                    (assoc obj key
                           (or (core/parse-int val)
                               val))
                    obj)
                  (if s-rest (str/join pattern' s-rest) pattern')
                  fmt-rest'))
         (assoc obj key (or (core/parse-int s) s))))
     obj)))

(defn parse
  "Smart parser. Can work with any of \"iso\" or \"ru\" formats. Returns datetime object.
   
   Lookup `datetime`."
  ([s fmt]
   (cond
     (map? s)
     s

     (not (string? s))
     nil

     (not (str/blank? s))
     (when fmt
       (let [date (parse* s fmt)]
         (cond-> date
           (:tz date)
           (assoc :tz (let [[_ sign hour min]
                            (re-find #"(\+|\-|Z)(\d{2}):?(\d{2})?$" s)]
                        (cond
                          (= (:tz date) "Z")
                          "Z"

                          sign
                          (str sign hour ":" min)

                          :else
                          (defaults :tz)))))))))
  ([s]
   (cond
     (map? s)
     s

     (not (string? s))
     nil

     (not (str/blank? s))
     (let [fmt (cond
                 (iso-fmt? s)
                 (iso-datetime-ms-with-tz)

                 (iso-fmt-lax? s)
                 (iso-datetime-ms-without-sec-with-tz)

                 (re-matches (re/re-conj [re/month "." re/year]) s)
                 [:month "." :year]

                 (re-matches re/ru-month-and-day s)
                 [:day "." :month]

                 (ru-fmt? s)
                 (ru-fmt)

                 (or
                  (re-matches (re/re-conj [re/hour ":" re/min]) s)
                  (re-matches (re/re-conj [re/hour ":" re/min ":" re/sec]) s))
                 [:hour ":" :min ":" :sec])]
       (parse s fmt)))))

;; Comparisons
;; -----------------------------------------

(defn eq?
  "Equal?
   
   Automatically parses strings.
   
   Has no respect to the timezones. Loses milliseconds."
  [x y]
  (apply = (map (comp parse #(dissoc % :tz :msec) parse) [x y])))

(defn lt?
  "Lower?
   
   Automatically parses strings.
   
   Has no respect to the timezones. Loses milliseconds."
  [x y]
  (apply u.ops/lt? (map (comp #(dissoc % :tz :msec) parse) [x y])))

(defn gt?
  "Greater?
   
   Automatically parses strings.
   
   Has no respect to the timezones. Loses milliseconds.
   
   Be careful with nil arguments. Nil is lesser than any datetime."
  [x y]
  (apply u.ops/gt? (map (comp #(dissoc % :tz :msec) parse) [x y])))

(defn lte?
  "Lower or equal?
   
   Automatically parses strings.
   
   Has no respect to the timezones. Loses milliseconds."
  [x y]
  (apply u.ops/lte? (map (comp #(dissoc % :tz :msec) parse) [x y])))

(defn gte?
  "Greater or equal?
   
   Automatically parses strings.
   
   Has no respect to the timezones. Loses milliseconds."
  [x y]
  (apply u.ops/gte? (map (comp #(dissoc % :tz :msec) parse) [x y])))

(defn in?
  "Date is in period?
   
   Bounds are inclusive."
  [{:keys [start end]} date]
  (and (or (nil? start)
           (gte? date start))
       (or (nil? end)
           (lte? date end))))

(defn compare-date
  "Sorts values in descending order."
  {:arglists '([x y]
               [key-fn x y])}
  ([x y]
   (cond
     (every? nil? [x y]) 0
     (nil? x) 1
     (nil? y) -1
     (= x y) 0
     (gt? x y) -1
     (lt? x y) 1
     :else (compare x y)))
  ([key-fn x* y*]
   (let [[x y] (map key-fn [x* y*])]
     (compare-date x y))))

(defn compare-date-asc
  "Sorts values in ascending order."
  {:arglists '([x y]
               [key-fn x y])}
  ([x y]
   (- (compare-date x y)))
  ([key-fn x* y*]
   (let [[x y] (map key-fn [x* y*])]
     (compare-date-asc x y))))

;; Operations
;; -----------------------------------------

(defn tz->utc
  "Moves `date` in the utc timezone. Returns new date in the same fmt.
   
   If `date` has no timezone, it will return as is, 'cuz it may be in any timezone."
  [date]
  (if (:tz (parse date))
    (let [fmt (when (any-fmt? date)
                (get-fmt-fn date))]
      (cond-> #?(:clj (try
                        (->> date iso-datetime-ms-with-tz
                             (jt/instant)
                             (.toString)
                             (parse))
                        (catch Exception _
                          date))
                 :cljs (let [utc-date    (-> date iso-datetime-ms-with-tz
                                             (js/Date.))]
                         (-> (. js/moment tz utc-date nil)
                             (.format)
                             (parse))))
        fmt fmt))
    date))

(defn tz->local
  "Moves `date` in the local timezone. Returns new date in the same fmt.
   
   If `date` has no timezone, it will return as is, 'cuz it may be in any timezone."
  [date]
  (if (:tz (parse date))
    (let [fmt (when (any-fmt? date)
                (get-fmt-fn date))]
      (cond-> #?(:clj (try
                        (let [zone-id
                              (or
                               (some-> (System/getenv "APP_TIMEZONE")
                                       (jt/system-clock)
                                       (jt/with-clock (jt/zone-id))) ; For production use
                               (jt/zone-id))                         ; For local use

                              offset-date-time
                              (-> date iso-datetime-ms-with-tz
                                  (jt/to-java-date)
                                  (jt/offset-date-time zone-id))

                              tz
                              (. offset-date-time getOffset)]
                          (-> (str offset-date-time)
                              (parse)
                              (assoc :tz (str tz))))
                        (catch Exception _
                          date))
                 :cljs (let [utc-date    (-> date iso-datetime-ms-with-tz
                                             (js/Date.))
                             current-tz (. (. js/moment -tz) guess)]
                         (-> (. js/moment tz utc-date current-tz)
                             (.format)
                             (parse))))
        fmt fmt))
    date))

(defn- family-equivalent
  "Returns family equivalent of the given fmt."
  {:arglists '([family fmt])}
  [family fmt*]
  (let [fmt (cond
              (symbol? fmt*)
              (get formats* fmt*)

              (string? fmt*)
              fmt*)]
    (->> formats*
         (filter (fn [[sym fmt-]]
                   (and (family (meta sym))
                        (apply = (map count [fmt- fmt])))))
         ffirst
         (get (ns-interns 'common.utils.date)))))

(defn date->iso
  "Returns same date in fmt that is ISO equivalent of given date fmt."
  [s]
  (when (string? s)
    (cond
      (wrong-tz-format? s) (-> s (parse) (iso-datetime-ms-with-tz))
      (iso-fmt? s)         s
      :else                ((family-equivalent :iso s) s))))

(defn- date-math
  "Performs `op` on date.
   
   `x` is date as string.
   
   `y` can be either date as string or interval as datetime object.
   
   Lookup `datetime`."
  [op x y]
  (if (is-datetime? x)
    (if (is-datetime? y)
      (op (parse x) (parse y))
      (let [fmt (get-fmt-fn x)]
        (cond-> (parse x)
          :always
          (op (with-meta y {:delta true}))
          fmt fmt)))
    nil))

(defn- valid?
  "Validates date part"
  [part val]
  (boolean
   (re-matches
    (get re/validators (symbol part))
    (as-datepart part val))))

(defn- set-part
  "Returns date in the same fmt as `date` which `part` value was set to `val`.
   
   If `val` is invalid returns nil."
  [date part val]
  (when (valid? part val)
    (let [fmt (get-fmt-fn date)]
      (cond-> (parse date)
        :always
        (assoc part val)

        fmt
        (fmt)))))

(defn set-parts
  "Returns date in the same fmt as `date` which parts where set to `kvs`."
  [date kvs]
  (let [fmt (get-fmt-fn date)]
    (cond-> (reduce-kv (fn [date part val]
                         (let [date' (set-part date part val)]
                           (if (nil? date')
                             (reduced date')
                             date')))
                       (parse date) kvs)
      fmt
      (fmt))))

(defn plus
  "Returns date in the same fmt as `date`.
   
   `date` is either string or datetime object.
   
   `interval` is datetime object.
   
   Lookup `datetime`."
  [date interval]
  (when (and (is-datetime? date)
             (map? interval))
    (date-math u.ops/plus date interval)))

(defn minus
  "Returns date in the same fmt as `date`.
   
   `date` is either string or datetime object.
   
   `interval` is datetime object.
   
   Lookup `datetime`."
  [date interval]
  (when (and (is-datetime? date)
             (map? interval))
    (date-math u.ops/minus date interval)))

(defn diff
  "Returns absolute difference between two dates. Returns interval as datetime object
   
   Dates can be either strings or datetime object.
   
   Lookup `datetime`."
  [x y]
  (let [defaults    {:year 0 :month 0 :day 0 :hour 0 :min 0 :sec 0}
        source-keys (-> x (parse) (keys))]
    (merge
     (select-keys defaults source-keys)
     (apply date-math u.ops/minus
            (if (gte? x y)
              [x y] [y x])))))

(defn day-diff
  [date1 date2]
  #?(:clj  (jt/time-between (->> date1 (jt/local-date "yyyy-MM-dd"))
                            (->> date2 (jt/local-date "yyyy-MM-dd"))
                            :days)
     ;; todo realize
     :cljs (throw js/Error)))

(defn- ru-month-name
  "Returns ru name of month when given number or its order when given string."
  [month]
  (let [ru-month ["января"
                  "февраля"
                  "марта"
                  "апреля"
                  "мая"
                  "июня"
                  "июля"
                  "августа"
                  "сентября"
                  "октября"
                  "ноября"
                  "декабря"]]
    (cond
      (string? month)
      (let [index (. ru-month indexOf month)]
        (when (not= -1 index)
          (inc index)))

      (number? month)
      (get ru-month (dec month)))))

(defn end-of-day
  "Returns datetime with 23:59:59 in ISO format."
  [date]
  (-> (set-parts date {:hour 23 :min 59 :sec  59})
      (iso-datetime)))

;; Now
;; -----------------------------------------

(defn utc-now
  "Returns UTC datetime as datetime object."
  []
  #?(:clj  (-> (java.time.Instant/now) str parse
               (dissoc :msec))
     :cljs (-> (. js/moment tz (js/Date.) nil)
               .format
               parse)))

(defn now
  "Returns local datetime as datetime object."
  []
  (tz->local (utc-now)))

(defn current-datetime
  "Returns local datetime in ISO format."
  []
  (iso-datetime-ms-with-tz (now)))

(defn current-date
  "Returns local date in ISO format."
  []
  (iso-date (now)))

(defn yesterday
  "Returns day before today in ISO format."
  []
  (-> (now)
      (minus {:day 1})
      (iso-date)))

(defn yesterday-datetime
  "Returns day before now in ISO datetime format."
  []
  (-> (now)
      (minus {:day 1})
      (iso-datetime)))

(defn tomorrow
  "Returns tomorrow date in ISO format."
  []
  (-> (now)
      (plus {:day 1})
      (iso-date)))

(defn tomorrow-datetime
  "Returns day after now in ISO datetime format."
  []
  (-> (now)
      (plus {:day 1})
      (iso-datetime)))

(defn tomorrow-datetime-with-tz
  "Returns day after now in ISO datetime format."
  []
  (-> (now)
      (plus {:day 1})
      (iso-datetime-with-tz)))

(defn end-current-datetime
  "Returns local end-of-day in ISO format."
  []
  (-> (now)
      (end-of-day)
      (iso-datetime)))

(defn next-day
  "Returns start of the next day in ISO datetime format."
  [date]
  (-> date
      (plus {:day 1})
      (set-parts {:hour 0 :min 0 :sec 0})
      (iso-datetime)))

(def week-days ["mon" "tue" "wed" "thu" "fri" "sat" "sun"])

(defn day-of-week
  []
  #?(:clj  (. (jt/day-of-week) getValue)
     :cljs (. (js/Date.) getDay)))

(defn day-of-week-short-name
  []
  (get week-days (dec (day-of-week))))

(def infinity (iso-datetime-ms-with-tz "9999-12-31T23:59:59.999Z"))

(defn age
  "Returns number of full years that passed since given date.
   
   Works only with ISO fmt."
  ([birth-date]
   (when (string? birth-date)
     (:year (diff (current-date) (format-date birth-date (iso-date))) 0)))
  ([birth-date specified-date]
   (when (and (string? birth-date) (string? specified-date))
     (:year (diff specified-date (format-date birth-date (iso-date))) 0))))

;; -----------------------------------------

'ok!
