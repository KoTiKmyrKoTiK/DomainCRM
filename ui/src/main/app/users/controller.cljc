(ns app.users.controller
  (:require [re-frame.core :refer [reg-event-fx]]

            [zframes.routing]

            [common.routes.users :as pid]))

(reg-event-fx
 pid/search
 (fn [{db :db} [pid phase fragment-params]]
   (case phase
     :init
     {:xhr/fetch {:uri "/api/users"
                  :req-id pid}}

     :deinit
     {:fx [[:dispatch [:xhr/deinit-everything pid]]]}

     nil)))