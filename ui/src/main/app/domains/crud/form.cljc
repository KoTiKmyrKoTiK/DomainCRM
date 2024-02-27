(ns app.domains.crud.form
  (:require [re-frame.core      :refer [reg-event-fx subscribe]]
            [zenform.model      :as zf]
            [zenform.validators :as zv]
            [clojure.string     :as str]))

(def form-path [:form :domains])

(def required-validator
  {:required {:message "Поле обязательно для заполнения"}})

(def default-items
  [{:value   nil
    :display "- Не выбрано -"}])

(def status-items
  (concat
   default-items
   [{:value   "new"
     :display "Не используется"}
    {:value   "active"
     :display "Используется"}
    {:value   "reserved"
     :display "Зарезервированный"}
    {:value   "archived"
     :display "Забаненный"}
    {:value   "archived"
     :display "Архивный"}]))

(def form-schema
  {:type   :form
   :fields {:id                        {:type :string}
            :domain                    {:type       :string
                                        :label      "Домен"
                                        :validators required-validator}
            :subdomain                 {:type  :string
                                        :label "Субдомен"}
            :server_ip                 {:type       :string
                                        :label      "IP сервера"
                                        :validators required-validator}
            :domain_server_provider_id {:type       :string
                                        :label      "Провайдер сервера"
                                        :validators (assoc-in required-validator [:required :message]
                                                              "Выберите значение")}
            :domain_provider_id        {:type       :string
                                        :label      "Провайдер домена"
                                        :validators (assoc-in required-validator [:required :message]
                                                              "Выберите значение")}
            :user_id                   {:type  :string
                                        :label "Пользователь"}
            :status                    {:type       :string
                                        :label      "Статус"
                                        :validators (assoc-in required-validator [:required :message]
                                                              "Выберите значение")
                                        :items      status-items}}})

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
