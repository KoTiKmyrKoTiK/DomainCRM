(ns app.users.crud.form
  (:require [re-frame.core      :refer [reg-event-fx subscribe]]
            [zenform.model      :as zf]
            [zenform.validators :as zv]
            [clojure.string     :as str]))

(def form-path [:form :users])

(def required-validator
  {:required {:message "Поле обязательно для заполнения"}})

(def role-items
  [{:value   "admin"
    :display "Администратор"}
   {:value   "buyer"
    :display "Байер"}
   {:value   "farmer"
    :display "Фармер"}])

(def form-schema
  {:type   :form
   :fields {:id          {:type       :integer}
            :name        {:type       :string
                          :label      "Имя пользователя"
                          :validators required-validator}
            :email       {:type       :string
                          :label      "E-Mail"
                          :validators (merge required-validator
                                             {:email {:message "Введите e-mail"}})}
            :password    {:type       :string
                          :label      "Пароль"
                          :validators required-validator}
            :re-password {:type       :string
                          :label      "Повторите пароль"
                          :validators (merge required-validator
                                             {::re-password {:message "Пароль не совпадает"}})}
            :role        {:type       :string
                          :label      "Роль"
                          :validators (assoc-in required-validator [:required :message]
                                                "Выберите значение")
                          :items      role-items}}})

(defmethod zv/validate
  ::re-password
  [{msg :message} v]
  (let [password @(subscribe [:zf/get-value form-path [:password]])]
    (when-not (= password v)
      (or msg "Пароль не совпадает"))))

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
     (if (seq errors)
       {:fx [#_[:dispatch [:close-loading]]]}
       {:dispatch [(:event success)
                   (-> (:params success)
                       (assoc :data (merge
                                     data
                                     {:form-value value})))]}))))
