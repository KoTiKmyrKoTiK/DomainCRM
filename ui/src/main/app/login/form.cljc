(ns app.login.form
  (:require [re-frame.core :as rf]
            [zenform.model :as zf]
            [clojure.string :as str]))

(def form-path [:form :login])

(def form-schema
  {:type :form
   :fields {:email    {:type       :string
                       :label      "E-Mail"
                       :validators {:required {:message "Поле обязательно для заполнения."}}}
            :password {:type       :string
                       :label      "Пароль"
                       :validators {:required {:message "Поле обязательно для заполнения."}}}}})

(rf/reg-event-fx
 ::init
 (fn [{db :db} [_ init]]
   {:dispatch [:zf/init form-path form-schema init]}))

(defn lower-case-username
  [form]
  (update form :userName str/lower-case))

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
