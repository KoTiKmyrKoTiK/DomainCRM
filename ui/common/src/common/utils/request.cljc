(ns common.utils.request
  (:require [clojure.data      :as data]
            [clojure.test      :refer [is]]
            [common.utils.core :as core]))

(defn- nullify
  "Sets all keys as nils."
  [map]
  (reduce-kv (fn [acc k v]
               (assoc acc k (when (map? v)
                              (nullify v))))
             {} map))

(defn- reflect
  "Dissocs all keys missing from pattern.
   Assocs keys missing from map as nils."
  [map pattern]
  (let [[a b _] (data/diff map pattern)]
    (-> map
        (core/dissoc-paths (core/get-all-paths a))
        (core/assoc-paths  (core/get-all-paths b)))))

(defn- merge-diff
  "A version of `common.utils/deep-merge` with custom sequential handling.
   
   All sequential things are treated as associative collections by their indexes, with results returned as vectors."
  [& maps]
  (letfn [(deep-merge*
           [a b]
           (loop [[[k bv :as i] & ks] b
                  acc a]
             (if (nil? i)
               acc
               (let [av (get a k)]
                 (if (= bv av)
                   (recur ks acc)
                   (recur ks (cond
                               (every? map? [bv av])
                               (assoc acc k (merge-diff av bv))

                               (every? sequential? [bv av])
                               (assoc acc k (mapv (fn [bv av]
                                                    (if (every? map? [bv av])
                                                      (merge-diff av bv)
                                                      (or bv av)))
                                                  bv av))

                               :else
                               (assoc acc k bv))))))))]
    (reduce deep-merge* maps)))

(defn nullify-maps-without-values
  "Sets all keys with map with only nil values as nils."
  [map]
  (reduce-kv
   (fn [acc k v]
     (let [new-v (cond (and (map? v)
                            (every? nil? (vals (nullify-maps-without-values v))))
                       nil

                       (map? v)
                       (nullify-maps-without-values v)

                       :else
                       v)]
       (assoc acc k new-v)))
   {}
   map))

(defn diff
  "Calculates diff between two forms.
   
   Returns form suitable for a `PATCH` request."
  [old new]
  (let [[old* new* both*] (data/diff old new)]
    (-> (merge-diff (nullify old*)
                    (reflect both* new*)
                    new*)
        (nullify-maps-without-values))))

;; Batchify
;; -------------------------------

(defn- calc-created-resources
  [old-resmap new-resource-type id new-resource & [_]]
  (when-not (get-in old-resmap [new-resource-type id])
    {:request  {:method "POST" :url (str "/" (name new-resource-type))}
     :resource new-resource}))

(defn- calc-changed-resources
  "Calculates diff for changed resources."
  [new-resmap old-resource-type id old-resource & [{:keys [without-meta?] :request/keys [use-patch?]}]]
  (let [new-resource    (get-in new-resmap [old-resource-type id])
        diff            (diff old-resource new-resource)]
    (cond (nil? new-resource)
          {:request {:method "DELETE" :url (str "/" (name old-resource-type) "/" (name id))}}

          (not= (cond-> new-resource        without-meta? (dissoc :meta))
                (cond-> old-resource without-meta? (dissoc :meta)))
          {:request  {:method (if use-patch?
                                "PATCH" "PUT")
                      :url    (str "/" (name old-resource-type) "/" (name id))}
           :resource (if use-patch?
                       diff new-resource)})))

(defn- reduce-resmap
  [resmap function & [params]]
  (reduce-kv
   (fn [acc resource-type resources]
     (->> resources
          (reduce-kv
           (fn [acc id resource]
             (if-let [r (function resource-type id resource params)]
               (conj acc r)
               acc))
           [])
          (into acc)))
   [] resmap))

(defn- resmap-diff
  "Calculates difference between two resmaps. Creates requests with appropriate methods accordingly.
   
   `POST` resources that exist only in `new`.
   
   `PUT` changed resources.
   
   `DELETE` resources that exist only in `old`."
  {:arglists '([old new & [{:keys [without-meta?] :request/keys [use-patch?]}]])}
  [old new & [params]]
  (let [changed
        (reduce-resmap old (partial calc-changed-resources new) params)
        created
        (reduce-resmap new (partial calc-created-resources old) params)]
    (into changed created)))

(defn- resmap
  "Creates map consisting of `{[resourceType id] resource}`.
   
   Gens random id for resources than miss it."
  {:arglists '([{:keys [entry]} & [{:keys [without-meta?]}]])
   :test     (fn []
               (is (= (resmap
                       {:entry [{:resource {:resourceType "Role" :id "role1"}}
                                {:resource {:resourceType "Role" :id "role2"}}
                                {:resource {:resourceType "User" :id "user"}}]})
                      {"Role" {"role2" {:resourceType "Role" :id "role2"}
                               "role1" {:resourceType "Role" :id "role1"}}
                       "User" {"user" {:resourceType "User" :id "user"}}})))}
  [data* & [{:keys [without-meta?]}]]
  (let [data (cond
               (:entry data*)
               (:entry data*)

               (sequential? data*)
               data*)]
    (reduce (fn [acc e*]
              (let [e (cond-> e*
                        (:resource e*)
                        (:resource e*)

                        (not without-meta?)
                        (dissoc :meta))]
                (cond-> acc
                  (:resourceType e)
                  (assoc-in [(:resourceType e) (or (:id e) (core/gen-uuid))] e))))
            {} data)))

(defn batchify
  "Creates a bundle of request with appropriate methods.
   
   `POST` resources that exist only in `new`.
   
   `PUT` changed resources. Can use `PATCH` instead when given key `request/use-patch`.
   
   `DELETE` resources that exist only in `old`."
  {:arglists '([old new & [{:keys [without-meta?] :request/keys [use-patch?]}]])}
  [old new & [params]]
  (let [use-patch-default false

        params'-1         (if (boolean? params)
                            {} params)
        use-patch?        (if (contains? params'-1 :request/use-patch)
                            (:request/use-patch params'-1) use-patch-default)
        without-meta?     ((some-fn :without-meta? true?) params)
        params'           (merge params'-1
                                 {:request/use-patch use-patch?
                                  :without-meta?     without-meta?})]
    {:resourceType "Bundle"
     :type "transaction"
     :entry (resmap-diff
             (resmap old params')
             (resmap new params')
             params')}))

;; -------------------------------
