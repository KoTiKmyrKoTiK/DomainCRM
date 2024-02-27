(ns app.domain-server-providers.crud.controller
  (:require [re-frame.core :refer [reg-event-fx]]

            [zframes.routing]

            [app.helpers :as h]

            [app.domain-server-providers.crud.form :as form]

            [common.routes.domain-server-providers :as pid]))

(reg-event-fx
 pid/create
 (fn [_ [pid phase _]]
   (case phase
     :init   {:fx [[:dispatch [::form/init]]]}
     :deinit {:dispatch [:xhr/deinit-everything [pid]]}
     nil)))

(reg-event-fx
 pid/edit
 (fn [_ [pid phase {:keys [id]}]]
   (case phase
     :init   {:fx [[:xhr/fetch {:uri     (str "/api/domain_server_providers/" id)
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
 ::save-flow
 (fn [{db :db} _]
   {:fx [[:dispatch [::form-eval {:success {:event ::upsert-resources}}]]]}))

(reg-event-fx ::form-eval form/form-eval)

(reg-event-fx
 ::upsert-resources
 (fn [{db :db} [_ payload]]
   (let [id (-> db :fragment-params :id)

         form-value (-> payload :data :form-value)]
     {:db (assoc-in db [pid/common :save-message] (if id "Провайдер сервера успешно сохранен" "Провайдер сервера успешно создан"))
      :xhr/fetch {:uri     (cond-> "/api/domain_server_providers"
                             id (str "/" id))
                  :method  (if id :PUT :POST)
                  :body    (cond-> form-value
                             (not id)
                             (dissoc :id))
                  :success {:event ::save-success}}})))

(reg-event-fx
 ::save-success
 (fn [{db :db} [_ _]]
   {:fx [[:dispatch [:flash/success {:header (get-in db [pid/common :save-message] "Документ сохранен")}]]
         [:dispatch [::h/redirect-to pid/search]]]}))