(ns common.utils.pre
  "Various conditions to use in `:pre` cause."
  (:require [clojure.string         :as str]
            [common.utils.core      :as core]
            [common.utils.date.pre  :as dh]))

(defn none?
  "Returns true if value is either nil or empty seq."
  [value]
  (cond
    (nil? value)
    true

    (string? value)
    (str/blank? value)

    (sequential? value)
    (empty? value)

    :else
    false))

(defn has?
  [& vals]
  (every? some? vals))

(defn datetime-or-nil?
  [value]
  (or
   (nil? value)
   (dh/any-fmt? value)))

(defn seq-or-nil?
  [value]
  (or (nil? value)
      (sequential? value)))

(defn map-or-nil?
  [value]
  (or (nil? value)
      (map? value)))

(defn string-or-nil?
  [value]
  (or (nil? value)
      (and (string? value)
           (not (str/blank? value)))))

(defn string->int?
  [value]
  (or (int? value)
      (and (string? value)
           (core/parse-int value))))

(defn number-or-nil?
  [value]
  (or (nil? value)
      (number? value)))

(defn is?
  [resourceType spec]
  (or (nil? spec)
      (and (map? spec)
           (= #{resourceType}
              (set
               (keys spec))))))

(defn codesystem?
  [value]
  (boolean
   (re-matches #"urn:CodeSystem:.*"
               (str value))))

(defn scalar?
  "String also considered scalar for the purpose of this fn."
  [value]
  (boolean
   ((some-fn number? string? keyword? symbol? true? false?)
    value)))

(defn period?
  [value]
  (cond
    (sequential? value)
    (every? datetime-or-nil? value)

    (map? value)
    (every? datetime-or-nil? (vals value))

    :else
    (datetime-or-nil? value)))
