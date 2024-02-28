(ns app.domains.controller
  (:require [re-frame.core :refer [reg-event-fx]]

            [zframes.routing]

            [app.domains.form      :as form]
            [app.domains.crud.form :as crud-form]

            [common.routes.domains :as pid]))

(reg-event-fx ::form-init form/form-init)

(reg-event-fx
 ::fetch-filter-items
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
   {:dispatch [:zf/update-node-schema form/filter-path [:domain-server-provider]
               {:items (map (fn [u] {:display (:name u)
                                     :value (:id u)})
                            data)}]}))

(reg-event-fx
 ::init-domain-providers-items
 (fn [_ [_ {data :data}]]
   {:dispatch [:zf/update-node-schema form/filter-path [:domain-provider]
               {:items (map (fn [u] {:display (:name u)
                                     :value (:id u)})
                            data)}]}))

(reg-event-fx
 ::init-users-items
 (fn [_ [_ {data :data}]]
   {:dispatch [:zf/update-node-schema form/filter-path [:user]
               {:items (map (fn [u] {:display (str (:name u) " (" (:email u) ")")
                                     :value (:id u)})
                            data)}]}))

(reg-event-fx
 pid/search
 (fn [{db :db} [pid phase fragment-params]]
   (case phase
     :init
     {:fx [[:dispatch [::form-init (:params fragment-params)]]
           [:dispatch [::fetch-filter-items]]
           [:xhr/fetch {:uri "/api/domains"
                        :req-id pid}]]}

     :deinit
     {:fx [[:dispatch [:xhr/deinit-everything pid]]]}

     nil)))