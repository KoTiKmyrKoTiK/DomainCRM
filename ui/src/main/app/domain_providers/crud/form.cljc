(ns app.domain-providers.crud.form
  (:require [re-frame.core      :refer [reg-event-fx subscribe]]
            [zenform.model      :as zf]
            [zenform.validators :as zv]
            [clojure.string     :as str]))

(def form-path [:form :domain-providers])

(def required-validator
  {:required {:message "Поле обязательно для заполнения"}})

(def form-schema
  {:type   :form
   :fields {:id   {:type :string}
            :name {:type       :string
                   :label      "Наименование"
                   :validators required-validator}}})

(reg-event-fx
 ::init
 (fn [{db :db} [_ init]]
   {:dispatch [:zf/init form-path form-schema init]}))

(defn form-eval
  [{db :db} [_ {:keys [success data]}]]
  (let [form-data                   (get-in db form-path)
        {:keys [errors value form]} (zf/eval-form form-data)]
    (merge
     {:db (assoc-in db form-path (assoc form :errors errors))}
     (when-not (seq errors)
       {:dispatch [(:event success)
                   (-> (:params success)
                       (assoc :data (merge
                                     data
                                     {:form-value value})))]}))))
