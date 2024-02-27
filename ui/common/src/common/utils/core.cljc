(ns common.utils.core
  "Namespace free of requires. Can be used anywhere."
  (:require [clojure.string :as str]
            #?@(:cljs [[goog.string]]))
  #?@(:clj ((:import (java.lang Integer Double))
            (:refer-clojure :exclude [format]))))

(defn rpartial
  "Reverse partial.
   
   Works ss `partial` but binds to the last arg."
  {:arglists '([f & args])}
  [f & args*]
  (fn [& args]
    (apply f (concat args args*))))

(defn format
  ^String [^String fmt & args]
  (apply
   #?(:clj  clojure.core/format
      :cljs goog.string/format)
   fmt
   args))

;; Parses
;; ----------------------------

(defn- parse-int*
  ^Integer [^String s]
  #?(:clj  (Integer/parseInt s)
     :cljs (js/parseInt s)))

(defn parse-int
  ^Integer [^String s]
  (when-let [x (re-matches #"[-+]?\d+" (str s))]
    (parse-int* x)))

(defn- parse-num*
  ^Number [^String s]
  #?(:clj  (Double/parseDouble s)
     :cljs (js/parseFloat s)))

(defn parse-num
  ^Number [^String s]
  (when-let [x (re-matches #"[-+]?[\d]+[.]?[\d]*" (str s))]
    (-> x
        (str/escape {\, \.})
        (parse-num*))))

;; Hashmap functions
;; ----------------------------

(defn dissoc-in
  "Removes empty nodes from the tree.
   
   Works only with maps."
  [map path]

  (loop [map  map
         path path]
    (let [node (get-in map (butlast path))]
      (cond
        (= 1 (count path))
        (dissoc map (last path))

        (and (map? node)
             (-> (dissoc node (last path))
                 keys seq))
        (update-in map (butlast path) dissoc (last path))

        :else
        (recur
         (update-in map (butlast path) dissoc (last path))
         (butlast path))))))

(defn get-all-paths
  "Returns all paths in form.
   
   Traverses maps and vectors."
  [form]
  (letfn [(get-paths
            [path form]
            (cond->> form
              (vector? form)
              ((fn [form]
                 (->> form
                      (map-indexed (fn [idx v]
                                     [idx v]))
                      (reduce (fn [acc [k v]]
                                (assoc acc k v))
                              {}))))

              :always
              (reduce-kv (fn [paths k v]
                           (let [path' (conj path k)]
                             (cond-> (conj paths path')
                               ((some-fn map? vector?) v)
                               (into (get-paths path' v)))))
                         [])))]
    (get-paths [] form)))

(defn dissoc-paths
  "Dissocs all paths from the map"
  [map paths]
  (reduce (fn [map path]
            (dissoc-in map path))
          map paths))

(defn assoc-paths
  "Assocs all paths from the map"
  [map paths]
  (reduce (fn [map path]
            (assoc-in map path nil))
          map paths))

;; Various
;; ----------------------------

(defn gen-uuid
  []
  (str #?(:clj  (java.util.UUID/randomUUID)
          :cljs (random-uuid))))

'ok!
