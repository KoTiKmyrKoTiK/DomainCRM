(ns app.login.controller
  (:require [re-frame.core :refer [reg-event-fx]]

            [app.login.form :as form]

            [zframes.auth   :as auth]
            [zframes.menu   :as menu]
            [zframes.routing]
            
            [common.routes.login :as pid]))

(reg-event-fx
 pid/login
 (fn [{db :db} [pid phase _]]
   (cond-> {:fx [[:dispatch [::form/init {:email "" :password ""}]]]
            :db db}
     (#{:deinit} phase)
     (update :db dissoc pid))))

(reg-event-fx
 ::submit
 (fn [{db :db} _]
   {:fx [[:dispatch [::form-eval {:success {:event ::try-login}}]]]}))

(reg-event-fx ::form-eval form/form-eval)

(reg-event-fx
 ::try-login
 (fn [{db :db} [_ {{{:keys [email password]} :form-value} :data}]]
   {:xhr/fetch {:uri "/api/login"
                :method :POST
                :body {:email email
                       :password password}
                :success {:event ::check-login-response}}}))

(reg-event-fx
 ::check-login-response
 (fn [{db :db} [_ {:keys [data]}]]
   (let [success? (:success data)]
     (if-not success?
       {:fx [[:dispatch [:flash/error {:header "Ошибка при авторизации" :body (:message data)}]]]}
       {:fx [[:dispatch [:flash/success {:header "Вы успешно авторизованы"}]]
             [:dispatch [::auth/signin-success data]]]}))))