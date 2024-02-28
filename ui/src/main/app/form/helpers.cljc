(ns app.form.helpers
  (:require [clojure.walk   :as walk]
            [clojure.string :as str]

            [re-frame.core :as rf]

            [chrono.core        :as ch]
            [zenform.model      :as zf]
            [zenform.validators :as validators]

            [common.utils.date :as cud]

            [app.helpers      :as h]))

(def form-path [:form :filter])

(def form-schema
  {:form-path form-path
   :schema    {:type   :form
               :fields {:search {:type :string}}}})

(rf/reg-event-fx
 ::reinit-form
 (fn [{db :db} [_ form-path form-schema ks]]
   (let [form-value (zf/get-value (get-in db form-path))]
     {:dispatch [:zf/init form-path form-schema (apply dissoc form-value ks)]})))

(defn make-params
  [params]
  (reduce-kv
   (fn [acc k v]
     (cond
       (:code v)
       (assoc acc k (:code v))

       (:id v)
       (assoc acc k (:id v))

       (str/includes? (name k) "-period")
       (cond-> acc
         (:start v) (assoc (keyword (str (name k) "-start")) (:start v))
         (:end v)   (assoc (keyword (str (name k) "-end")) (:end v)))

       (map? v)
       (merge acc (make-params v))

       (vector? v)
       (assoc acc k (->> v (map :id) (str/join ",")))

       (false? v)
       acc

       :else
       (assoc acc k (:value v v))))
   {}
   params))

(defn label-value
  [form]
  (clojure.walk/prewalk
   (fn [x]
     (cond
       (and (map? x) (:value x) (:label x))
       (select-keys x [:label :value :items :auto :count?])

       (and (map? x) (:value x) (not (:display x)))
       (:value x)

       :else
       x)) form))

(rf/reg-event-fx
 ::validate-filter-form
 (fn [{db :db} [_ {:keys [success data]}]]
   (let [form-path' (or (:form-path data) form-path)
         form-data  (get-in db form-path')
         {:keys [errors value form]} (zf/eval-form form-data)
         params ((if-let [make-params-fn (:make-params-fn data)] make-params-fn make-params) value)]
     (cond
       (empty? errors)
       {:fx [[:dispatch [:zframes.redirect/set-params params]]
             (when (:event success)
               [:dispatch [(:event success) (-> (:params success)
                                                (assoc :data (merge
                                                              data
                                                              {:form-data  (label-value form)
                                                               :form-value value
                                                               :params     params})))]])]
        :db (assoc-in db (conj form-path' :success) true)}

       (seq errors)
       {:db (-> db
                (assoc-in form-path' (assoc form :errors errors))
                (assoc-in (conj form-path' :success) false))}

       (empty? params)
       {:dispatch [:zframes.redirect/set-params params]
        :db       (assoc-in db (conj form-path' :success) false)}))))

(rf/reg-event-fx
 ::clear-node
 (fn [{db :db} [_ _ form-path _ pths]]
   (let [form (get-in db form-path)]
     {:db (assoc-in db form-path
                    (reduce (fn [form path]
                              (update-in form (zf/get-node-path path)
                                         dissoc :value))
                            form pths))})))

(defn update-validators
  [form-path' paths validator]
  (reduce
   (fn [acc path]
     (conj acc [:dispatch [:zf/update-node-schema (or form-path' form-path) path validator]]))
   []
   paths))

(def regex-char-esc-smap
  (let [esc-chars "()*&^%$#!"]
    (zipmap esc-chars
            (map #(str "\\" %) esc-chars))))

(defn str-to-pattern
  [string]
  (->> string
       (remove #{\\})
       (replace regex-char-esc-smap)
       (apply str)
       (str "(?i)")
       re-pattern))

(defn highlight-search
  [search display]
  (if (str/blank? search)
    display
    (str/replace display
                 (str-to-pattern search)
                 #(str "<mark>" % "</mark>"))))

(defn get-indexed-value
  ([form-data]
   (some->> form-data
            (reduce-kv (fn [acc k v] (assoc acc k (zf/get-value v))) {})))
  ([form-data path]
   (some-> form-data
           (get-in (zf/get-value-path path))
           (->> (reduce-kv (fn [acc k v] (assoc acc k (zf/get-value v))) {})))))

(defn section-with-error
  [section-fields-mapping errors]
  (->> (reduce (fn [acc val]
                 (assoc acc
                        (some (fn [[k v]]
                                (when (and (vector? v) ((set v) (ffirst val)))
                                  k))
                              acc)
                        true))
               section-fields-mapping
               errors)
       (filter (fn [[_ v]] (true? v)))
       (keys)
       (filter identity)))

(defn humanize-form-errors
  [form-data errors]
  (->> errors
       (reduce-kv
        (fn [acc path err]
          (conj acc
                (let [value   (zf/get-value form-data path)
                      is-seq? (or
                               (vector? value)
                               (map?    value)
                               (list?   value))]
                  (h/format-str
                   (if is-seq?
                     "Ошибка: %s по значениям: %s"
                     "Ошибка: %s по значению: %s")
                   (->> err (vals) (str/join "; "))
                   (if is-seq?
                     (->> value
                          (reduce-kv (fn [v-acc v-k v-v]
                                       (conj v-acc (h/format-str "%s: %s" (if (keyword?  v-k)
                                                                            (name      v-k)
                                                                            (str v-k)) v-v)))
                                     [])
                          (str/join "; "))
                     value)))))
        [])))

(defn init-vector-value
  [items value]
  (some-> value
          (str/split ",")
          (->> (mapv (fn [i]
                       (->> items
                            (h/find-first #(= i (get-in % [:value :id])))
                            :value))))))

(rf/reg-event-fx
 ::clear-node
 (fn [{db :db} [_ _ form-path _ pths]]
   (let [form (get-in db form-path)]
     {:db (assoc-in db form-path
                    (reduce (fn [form path]
                              (update-in form (zf/get-node-path path)
                                         dissoc :value))
                            form pths))})))

(defn humanize-filters
  [filters]
  (->> filters
       (remove (fn [[_ v]]
                 (if (= (or (:filter-type v) (:type v)) :period)
                   (every? nil? [(get-in v [:value :start :chrono-value])
                                 (get-in v [:value :end :chrono-value])])
                   (not (:value v)))))
       (map
        (fn [[_ v]]
          (let [value   (:value v)
                display (case (or (:filter-type v) (:type v))
                          :datetime
                          (cud/ru-datetime-short (or value (:default v)))

                          :date
                          (cud/ru-date (or value (:default v)))

                          :period
                          (h/format-str "%s — %s"
                                        (h/identity-default "..." (some-> v (zf/get-value [:start]) (cud/ru-date)))
                                        (h/identity-default "..." (some-> v (zf/get-value [:end])   (cud/ru-date))))
                          :object
                          (-> v :value :display)

                          :string
                          (if (:items v)
                            (->> (:items v)
                                 (h/find-first #(= value (:value %)))
                                 :display)
                            value)

                          :boolean
                          (if value "Да" "Нет")

                          :vector
                          (if (string? value)
                            (some-> value
                                    (str/split ",")
                                    (->> (map (fn [i]
                                                (->> v :items
                                                     (h/find-first #(= i (get-in % [:value :id])))
                                                     :value :display)))
                                         (str/join ", ")))
                            (->> value (map :display) (str/join ", ")))

                          value)]
            {:title    (:label v)
             :display  display})))))

(rf/reg-event-db
 ::clear-value
 (fn [db [_ value-type form-path path]]
   (cond-> db
     (not (#{:period :date :time} value-type))
     (update-in form-path (fn [form] (zf/set-value form form-path [path] nil))))))

(rf/reg-event-fx
 ::clear-filter-value
 (fn [{db :db} [_ form-path' path node & [{:keys [remove-chips-ev]}]]]
   (let [params   (->> (get-in db [:fragment-params :params])
                       (reduce-kv (fn [acc k v]
                                    (cond
                                      (and (= :form (:type node))
                                           (str/starts-with? (name k) (name path))
                                           (some (fn [[subpath _]]
                                                   (str/ends-with? (name k) (name subpath)))
                                                 (:value node)))
                                      (let [subnode (->> node :value
                                                         (keep (fn [[subpath node]]
                                                                 (when (str/ends-with? (name k) (name subpath))
                                                                   node)))
                                                         (first))]
                                        (assoc acc k (:default subnode)))

                                      (not (str/starts-with? (name k) (name path)))
                                      (assoc acc k v)

                                      :else
                                      (assoc acc k (:default node))))
                                  {})
                       (h/strip-nils))]
     {:fx (cond->
           [[:dispatch [::clear-value
                        (or (:filter-type node) (:type node))
                        form-path'
                        path]]
            [:dispatch [:zframes.redirect/set-params params]]]

            remove-chips-ev
            (conj [:dispatch remove-chips-ev]))
      :db (if (empty? params)
            (update-in db form-path' dissoc :success)
            db)})))