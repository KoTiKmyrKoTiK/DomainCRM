(ns app.domain-server-providers.controller
  (:require [re-frame.core :refer [reg-event-fx]]

            [zframes.routing]

            [common.routes.domain-server-providers :as pid]))

(reg-event-fx
 pid/search
 (fn [{db :db} [pid phase fragment-params]]
   (case phase
     :init
     {:xhr/fetch {:uri "/api/domain_server_providers"
                  :req-id pid}}

     :deinit
     {:fx [[:dispatch [:xhr/deinit-everything pid]]]}

     nil)))