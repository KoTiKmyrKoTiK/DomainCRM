(ns common.utils.mapper
  (:refer-clojure :exclude [import get-in]))

(declare get-in)

(declare import)
(declare export)

(declare get-from)
(declare set-to)

(declare query)

;; Inner
;; ------------------

(defn- operand
  [ex]
  (fn [e]
    (if (coll? ex)
      (get-from e ex)
      (if (keyword? ex)
        (get e ex)
        ex))))

(def ^:private cmp
  {:=    =
   :not= not=
   :<    <
   :>    >
   :<=   <=
   :>=   >=})

(defn- pred
  [[op l r]]
  (fn [v]
    ((get cmp op)
     ((operand l) v) ((operand r) v))))

(defn- comp-expr
  [expr]
  (fn [coll]
    (filterv (pred expr) coll)))

(defn- and-expr
  [[op & exprs]]
  (fn [coll]
    (reduce #(%2 %1)
            coll
            (map comp-expr exprs))))

(defn- or-expr
  [[op & exprs]]
  (fn [coll]
    (reduce #(vec (concat %1 (%2 coll)))
            []
            (map comp-expr exprs))))

(defn- not-expr
  [[op l]]
  (fn [coll]
    (filter #(not ((operand l) %)) coll)))

(defn- contains-predicate
  [[_ l r]]
  #(contains? (set r) ((operand l) %)))

(defn- in-expr
  [args]
  (fn [coll]
    (filter (contains-predicate args) coll)))

(defn- not-in-expr
  [args]
  (fn [coll]
    (remove (contains-predicate args) coll)))

(defn query
  "Returns all elements that match `expr`."
  [data expr]
  (case (first expr)
    :and    ((and-expr expr) data)
    :or     ((or-expr expr) data)
    :not    ((not-expr expr) data)
    :in     ((in-expr expr) data)
    :not-in ((not-in-expr expr) data)
    ((comp-expr expr) data)))

(defn- idx-of
  [x coll]
  (first (keep-indexed #(when (= %2 x) %1) coll)))

(defn- find-idx
  [m expr]
  (idx-of (first (query m expr)) m))

(defn get-from
  {:arglists '([data [k & ks :as path]])}
  [data path]
  (reduce
   (fn [acc p]
     (if (map? p)
       (cond
         (:map p)
         (if (sequential? acc)
           (mapv #(export % (:map p)) acc)
           (export acc (:map p)))

         (:- p)
         acc

         :else
         (let [res (vec (query acc (:get p)))]
           (if (empty? res)
             (:set p)
             res)))
       (if (and (= p :#) (sequential? acc))
         acc
         (if (and (sequential? acc) (keyword? p))
           (mapv p acc)
           (get acc p)))))
   data path))

(defn deep-merge
  "efficient deep merge"
  [a b]
  (loop [[[k v :as i] & ks] b
         acc a]
    (if (nil? i)
      acc
      (let [av (get a k)]
        (if (= v av)
          (recur ks acc)
          (recur ks (if (and (map? v) (map? av))
                      (assoc acc k (deep-merge av v))
                      (assoc acc k v))))))))

(defn set-to
  [data [k & ks] value]
  (let [v (cond
            ks
            (cond
              (and (= :# (first ks)) (sequential? value))
              (vec (map (fn [x] (set-to nil (rest ks) x)) value))

              (map? k)
              (cond
                (:get k) (set-to (query data (:get k)) ks value)
                :else nil)

              :else
              (set-to (get data k) ks value))

            (:map k)
            (import value (:map k))

            (:- k)
            (let [dissoc-keys (:- k)]
              (apply dissoc value (if (vector? dissoc-keys) dissoc-keys [dissoc-keys])))

            :else
            value)]
    (cond
      (integer? k)
      (assoc (or data (vec (repeat k nil))) k v)

      (map? k)
      (let [set (:set k)
            v   (if (sequential? v)
                  (mapv (fn [x]
                          (deep-merge set x))
                        v)
                  (deep-merge set v))
            should-collection (:get k)
            idx (when-let [getter (:get k)]
                  (find-idx data getter))]
        (if should-collection
          (if idx
            (if (sequential? v)
              (assoc data idx  (first v))
              (assoc data idx  v))
            (if (sequential? v)
              (vec (concat (or data []) v))
              (conj (or data []) v)))
          v))

      (every? sequential? [data v])
      (vec (concat data v))

      (every? nil? [data k])
      v

      (and (sequential? data) (= :# k))
      (conj (or data []) v)

      :else
      (assoc data k v))))

(comment
  (import
   {:address {:text    "1"
              :manual? true}}
   [[[:address] [:address {:- [:manual?]}]]]))

(defn import
  [data mapping]
  (reduce
   (fn [acc [to from]]
     (let [data (get-from data from)]
       (if (some? data)
         (set-to acc to data)
         acc)))
   {} mapping))

(defn export
  [data mapping & [default]]
  (reduce
   (fn [acc [from to]]
     (let [data (get-from data from)]
       (if (some? data)
         (set-to acc to data)
         acc)))
   (or default {}) mapping))

(defn get-in
  {:arglists '([data [k & ks :as path]]
               [data [k & ks :as path] not-found])}
  ([data path] (get-from data path))
  ([data path not-found]
   (or (get-in data path) not-found)))
