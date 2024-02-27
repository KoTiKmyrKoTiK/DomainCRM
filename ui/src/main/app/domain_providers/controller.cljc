(ns app.domain-providers.controller
  (:require [re-frame.core :refer [reg-event-fx]]

            [zframes.routing]

            [common.routes.domain-providers :as pid]))

(reg-event-fx
 pid/search
 (fn [{db :db} [pid phase fragment-params]]
   (case phase
     :init
     {:xhr/fetch {:uri "/api/domain_providers"
                  :req-id pid}}

     :deinit
     {:fx [[:dispatch [:xhr/deinit-everything pid]]]}

     nil)))