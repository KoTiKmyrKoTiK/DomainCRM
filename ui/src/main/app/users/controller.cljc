(ns app.users.controller
  (:require [re-frame.core :refer [reg-event-fx]]

            [zframes.routing]

            [common.routes :as cr]))

(reg-event-fx
 cr/pid-users
 (fn [{db :db} [pid phase fragment-params]]
   (case phase
     :init
     {:xhr/fetch {:uri "/api/users"
                  :req-id pid}}

     :deinit
     {:fx [[:dispatch [:xhr/deinit-everything pid]]]}

     nil)))