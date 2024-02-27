(ns app.domains.crud.controller
  (:require [re-frame.core :refer [reg-event-fx]]

            [zframes.routing]

            [app.helpers :as h]

            [app.domains.crud.form :as form]

            [common.routes.domains :as pid]))

(reg-event-fx
 ::fetch-dropdown-items
 (fn [_ _]
   {:xhr/fetch [{:uri "/api/users"
                 :success {:event ::init-users-items}}
                {:uri "/api/domain_server_providers"
                 :success {:event ::init-domain-server-providers-items}}
                {:uri "/api/domain_providers"
                 :success {:event ::init-domain-providers-items}}]}))

(reg-event-fx
 ::init-domain-server-providers-items
 (fn [_ [_ {data :data}]]
   {:dispatch [:zf/update-node-schema form/form-path [:domain_server_provider_id]
               {:items (->> data
                            (map (fn [u] {:display (:name u)
                                          :value (:id u)}))
                            (concat form/default-items))}]}))

(reg-event-fx
 ::init-domain-providers-items
 (fn [_ [_ {data :data}]]
   {:dispatch [:zf/update-node-schema form/form-path [:domain_provider_id]
               {:items (->> data
                            (map (fn [u] {:display (:name u)
                                          :value (:id u)}))
                            (concat form/default-items))}]}))

(reg-event-fx
 ::init-users-items
 (fn [_ [_ {data :data}]]
   {:dispatch [:zf/update-node-schema form/form-path [:user_id]
               {:items (->> data
                            (map (fn [u] {:display (str (:name u) " (" (:email u) ")")
                                          :value (:id u)}))
                            (concat form/default-items))}]}))

(reg-event-fx
 pid/create
 (fn [_ [pid phase _]]
   (case phase
     :init   {:fx [[:dispatch [::fetch-dropdown-items]]
                   [:dispatch [::form/init]]]}
     :deinit {:dispatch [:xhr/deinit-everything [pid]]}
     nil)))

(reg-event-fx
 pid/edit
 (fn [_ [pid phase {:keys [id]}]]
   (case phase
     :init   {:fx [[:dispatch [::fetch-dropdown-items]]
                   [:xhr/fetch {:uri     (str "/api/domains/" id)
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
     {:db (assoc-in db [pid/common :save-message] (if id "Домен успешно сохранен" "Домен успешно создан"))
      :xhr/fetch {:uri     (cond-> "/api/domains"
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
         [:dispatch [::h/redirect-to pid/search]]]}))