(ns app.home.controller
  (:require [re-frame.core :refer [reg-event-fx]]

            [common.routes.common :as pid]))

(reg-event-fx
 pid/dashboard
 (fn [_ _]
   {}))