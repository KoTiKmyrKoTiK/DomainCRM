(ns app.domains.controller
  (:require [re-frame.core :refer [reg-event-fx]]

            [zframes.routing]

            [app.domains.form :as form]

            [common.routes.domains :as pid]))

(reg-event-fx ::form-init form/form-init)

(reg-event-fx
 pid/search
 (fn [{db :db} [pid phase fragment-params]]
   (case phase
     :init
     {:fx [[:dispatch [::form-init (:params fragment-params)]]
           [:xhr/fetch {:uri "/api/domains"
                        :req-id pid}]]}

     :deinit
     {:fx [[:dispatch [:xhr/deinit-everything pid]]]}

     nil)))