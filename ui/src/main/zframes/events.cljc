(ns zframes.events
  (:require [clojure.set :as set]

            [re-frame.core  :as rf]
            [re-frame.utils :as rfu]

            [zframes.mapper :as zm]
            [zenform.model  :as zf]

            [app.helpers :as h]))

;; TODO to zframes
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

(defn deep-merge-concat
  [a b]
  (merge-with
   (fn [x y]
     (cond (map? y)        (deep-merge x y)
           (sequential? y) (into x y)
           :else           y))
   a b))

(defn calc-created-resources
  [old-resmap new-resource-type id new-resource]
  (when-not (get-in old-resmap [new-resource-type id])
    {:request  {:method "POST" :url (str "/" (name new-resource-type))}
     :resource new-resource}))

(defn calc-changed-resources
  [new-resmap old-resource-type id old-resource]
  (let [match (get-in new-resmap [old-resource-type id])]
    (cond (nil? match)
          {:request {:method "DELETE" :url (str "/" (name old-resource-type) "/" (name id))}}

          (not= match old-resource)
          {:request  {:method "PUT" :url (str "/" (name old-resource-type) "/" (name id))}
           :resource match})))

(defn reduce-resmap
  [resmap function]
  (reduce-kv (fn [acc resource-type resources]
               (->> resources
                    (reduce-kv (fn [acc id resource]
                                 (if-let [r (function resource-type id resource)]
                                   (conj acc r)
                                   acc))
                               [])
                    (into acc)))
             [] resmap))

(defn resmap-diff
  "Calculates difference bewteen two resmaps. Creates requests with appropriate methods accordingly.
   
   `POST` resources that exist only in `new`.
   
   `PUT` changed resources.
   
   `DELETE` resources that exist only in `old`."
  [old new]
  (let [changed (reduce-resmap old (partial calc-changed-resources new))
        created (reduce-resmap new (partial calc-created-resources old))]
    (into changed created)))

(defn resmap
  "Creates map consisting of `{[resourceType id] resource}`."
  [{:keys [entry]}]
  (reduce
   (fn [acc {e :resource}]
     (let [e (dissoc e :meta)]
       (cond-> acc
         (:resourceType e)
         (assoc-in [(:resourceType e) (or (:id e) (str #?(:clj (java.util.UUID/randomUUID)
                                                          :cljs (random-uuid))))] e))))
   {}
   entry))

(defn batchify
  "Creates a bundle of request with appropriate methods.
   
   `POST` resources that exist only in `new`.
   
   `PUT` changed resources.
   
   `DELETE` resources that exist only in `old`."
  [old new]
  {:resourceType "Bundle"
   :type "transaction"
   :entry (resmap-diff (resmap old) (resmap new))})

(defn event
  [action & [data]]
  (cond
    (map? action)
    [(:event action)
     (cond-> (:params action)
       (map? (:params action {}))
       (merge {:data data}))]))

(rf/reg-event-fx
 ::init
 (fn [_ [_ {{:keys [form-path form-schema mapper]} :params, :keys [data success]}]]
   {:fx (cond-> [[:dispatch (cond-> [:zf/init form-path form-schema]
                              data  (conj (cond-> data mapper (zm/import mapper))))]]
          success
          (conj [:dispatch (event success)]))}))

(rf/reg-event-fx
 ::eval
 (fn [{db :db} [_ {{:keys [mapper form-path]} :params, :keys [success error]}]]
   (let [{:keys [errors value form]} (zf/eval-form (get-in db form-path))]
     (merge
      {:db (assoc-in db form-path
                     (cond-> form
                       (seq errors)
                       (assoc :errors errors)))}
      (if (empty? errors)
        (when success
          {:dispatch (event success (cond-> value mapper (zm/export mapper)))})
        (when error
          {:dispatch (event error errors)}))))))

(rf/reg-event-fx
 ::deinit
 (fn [{db :db} [_ {{:keys [form-path]} :params, :keys [data success]}]]
   (cond-> {}
     form-path
     (assoc :db (rfu/dissoc-in db form-path))

     success
     (assoc :dispatch (event success data)))))

(rf/reg-event-fx
 ::error
 (fn [_ [_ data params]]
   (let [success (or (:success data) (:success params))
         errors  (or (when-not (map? data) data)
                     (:data data))]
     (cond->
      {:fx (mapv
            (fn [[_ configuration]]
              [:flash/flash [:danger {:msg (first (vals configuration))}]])
            errors)}
       success (assoc :dispatch (event success))))))

(rf/reg-event-fx
 ::batch
 (letfn [(entry
           [resources]
           (let [resources (cond-> resources (map? resources) vector)]
             {:entry (mapv (partial hash-map :resource)
                           resources)}))]
   (fn [_ [_ {:keys [data success error params]}]]
     {:fhir/create {:resource (batchify
                               (entry (:old params))
                               (entry data))
                    :success  success
                    :error    error}})))

(rf/reg-event-fx
 ::conj-uri
 (fn [_ [_ {:keys [success params]}]]
   {:zframes.redirect/redirect {:uri (apply str (:fragment params) (:path params) (:params params))}
    :dispatch (event success)}))

(rf/reg-event-fx
 ::flash
 (fn [_ [_ {:keys [success params data] :as ss}]]
   (cond-> {:flash/flash [(:type params) {:msg (:msg params)}]}
     success
     (assoc :dispatch (event success data)))))

(rf/reg-event-fx
 ::format-response
 (fn [_ [_ resp {:keys [success]}]]
   {:dispatch (event success resp)}))

(rf/reg-event-fx
 ::skip-response
 (fn [_ [_ _ {:keys [success]}]]
   {:dispatch (event success)}))

(rf/reg-event-fx
 ::ignore-response
 (fn [_ [_ _ {:keys [success data]}]]
   {:dispatch [(:event success) (assoc (:params success) :data data)]}))

(rf/reg-event-fx
 ::do-events
 (fn [_ [_ {:keys [data success]}]]
   {:fx (mapv (fn [ev]
                [:dispatch (event ev data)])
              success)}))

(rf/reg-event-fx
 ::format-success-event
 (fn [_ [_ resp {:keys [success data]}]]
   {:dispatch [(:event success)
               (assoc (:params success) :data
                      (merge data (:data resp)))]}))

(rf/reg-event-fx
 ::dispatch-modal
 (fn [_ [_ modal]]
   {:dispatch modal}))

(rf/reg-event-fx
 ::read->update
 (fn [_ [_ resource-to-update payload]]
   {:fhir/get {:resource (select-keys resource-to-update [:resourceType :id])
               :success  {:event  ::patch-after-read
                          :params (assoc-in payload [:data :resource-to-update] resource-to-update)}
               :error    (:error payload)}}))

(rf/reg-event-fx
 ::read->check->update
 (fn [_ [_ resource-to-update payload]]
   {:fhir/get {:resource (select-keys resource-to-update [:resourceType :id])
               :success  {:event  ::check-on-recent-change
                          :params (assoc-in payload [:data :resource-to-update] resource-to-update)}}}))

(rf/reg-event-fx
 ::check-on-recent-change
 (fn [_ [_ {resource :data :as resp} {:keys [data] :as payload}]]
   (if (not= (get-in resource [:meta :versionId]) (get-in data [:meta :versionId]))
     {:dispatch [:flash/warning {:msg "Обнаружен конфликт при сохранении данных. Для продолжения обновите страницу"}]}
     {:dispatch [::patch-after-read resp payload]})))

(rf/reg-event-fx
 ::patch-after-read
 (fn [_ [_ {resource :data} {:keys [error data] :as payload}]]
   (letfn [(merge-resource
             [initial-resource]
             (reduce-kv
              (fn [acc k v]
                (let [new-value (get-in data [:resource-to-update k])]
                  (assoc acc k
                         (cond
                           (and (:replace? payload) (vector? v))
                           (let [value (h/vectorize new-value)]
                             (-> (remove
                                  #(let [match-fields [:noteType :system]]
                                     (->> value
                                          (map (fn [v] (h/get-value-by-keys v match-fields)))
                                          (some (partial = (h/get-value-by-keys % match-fields)))))
                                  v)
                                 (vec)
                                 (into value)))

                           (vector? v)
                           (conj v new-value)

                           :else
                           new-value))))
              {}
              initial-resource))]
     (let [fields-to-update
           (-> data :resource-to-update
               (keys)
               (set))

           exist-fields
           (-> resource
               (select-keys fields-to-update)
               (keys)
               (set))

           new-fields
           (set/difference fields-to-update exist-fields)

           resource-to-update
           (cond-> (->> (select-keys resource fields-to-update)
                        (merge-resource))
             (seq new-fields)
             (merge (select-keys (:resource-to-update data) new-fields)))]
       {:fhir/patch {:resource resource-to-update
                     :success  {:event  ::ignore-response
                                :params payload}
                     :error    error}}))))
