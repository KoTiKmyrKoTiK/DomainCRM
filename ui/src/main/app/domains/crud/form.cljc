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
    :display "-Не выбрано-"}])

(def status-items
  (concat
   default-items
   [{:value   "new"
     :display "Не используется"}
    {:value   "active"
     :display "Используется"}
    {:value   "reserved"
     :display "Зарезервированный"}
    {:value   "restricted"
     :display "Забаненный"}
    {:value   "archived"
     :display "Архивный"}]))

(def re-domain #"^(([a-zA-Z]{1})|([a-zA-Z]{1}[a-zA-Z]{1})|([a-zA-Z]{1}[0-9]{1})|([0-9]{1}[a-zA-Z]{1})|([a-zA-Z0-9][a-zA-Z0-9-_]{1,61}[a-zA-Z0-9]))\.([a-zA-Z]{2,6}|[a-zA-Z0-9-]{2,30}\.[a-zA-Z]{2,3})$")
(def re-subdomain #"^[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9]$")
(def re-ip #"^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$")

(def form-schema
  {:type   :form
   :fields {:id                        {:type :string}
            :domain                    {:type       :string
                                        :label      "Домен"
                                        :validators (assoc required-validator
                                                           :pattern {:regex   re-domain
                                                                     :message "Домен невалиден"})}
            :subdomain                 {:type       :string
                                        :label      "Субдомен"
                                        :validators {:pattern {:regex   re-subdomain
                                                               :message "Субдомен невалиден"}}}
            :server_ip                 {:type       :string
                                        :label      "IP сервера"
                                        :validators (assoc required-validator
                                                           :pattern {:regex   re-ip
                                                                     :message "IP невалиден"})}
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
