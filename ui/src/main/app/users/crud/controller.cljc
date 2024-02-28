(ns app.users.crud.controller
  (:require [re-frame.core :refer [reg-event-fx]]

            [zframes.routing]

            [app.helpers :as h]

            [app.users.crud.form :as form]

            [common.routes.users :as pid]))

(reg-event-fx
 pid/create
 (fn [_ [pid phase _]]
   (case phase
     :init   {:fx [[:dispatch [::form/init {:status "active"}]]]}
     :deinit {:dispatch [:xhr/deinit-everything [pid]]}
     nil)))

(reg-event-fx
 pid/edit
 (fn [_ [pid phase {:keys [id]}]]
   (case phase
     :init   {:fx [[:xhr/fetch {:uri     (str "/api/users/" id)
                                :success {:event ::edit-page-init}
                                :error   {:event ::h/errored}
                                :req-id  pid}]]}
     :deinit {:dispatch [:xhr/deinit-everything [pid]]}
     nil)))

(reg-event-fx
 ::edit-page-init
 (fn [{db :db} [_ {:keys [data]}]]
   {:fx [[:dispatch [::form/init data]]]}))

(reg-event-fx
 ::update-user-status
 (fn [{db :db} [_ user status]]
   (let [userinfo (-> db :zframes.auth/userinfo)]
     (if (and (= (:id user) (:id userinfo))
              (#{"restricted"} status))
       {:dispatch [:flash/error {:header "Вы не можете заблокировать себя!"}]}
       {:db (assoc-in db [pid/common :save-message]
                      (if (#{"restricted"} status)
                        "Пользователь успешно заблокирован"
                        "Пользователь успешно разблокирован"))
        :xhr/fetch {:uri     (str "/api/users/" (:id user))
                    :method  :PUT
                    :body    (assoc user :status status)
                    :success {:event ::save-success}}}))))

(reg-event-fx
 ::save-flow
 (fn [{db :db} _]
   {:fx [[:dispatch [::form-eval {:success {:event ::upsert-resources}}]]]}))

(reg-event-fx ::form-eval form/form-eval)

(reg-event-fx
 ::upsert-resources
 (fn [{db :db} [_ payload]]
   (let [id (-> db :fragment-params :id)

         form-value (-> payload :data :form-value)]
     {:db (assoc-in db [pid/common :save-message] (if id "Пользователь успешно сохранен" "Пользователь успешно создан"))
      :xhr/fetch {:uri     (cond-> "/api/users"
                             id (str "/" id))
                  :method  (if id :PUT :POST)
                  :body    (cond-> form-value
                             :always
                             (dissoc :re-password)
                             
                             (not id)
                             (dissoc :id))
                  :success {:event ::save-success}}})))

(reg-event-fx
 ::save-success
 (fn [{db :db} [_ _]]
   {:fx [[:dispatch [:flash/success {:header (get-in db [pid/common :save-message] "Документ сохранен")}]]
         [:dispatch [:zframes.redirect/redirect {:uri "#/users"}]]]}))