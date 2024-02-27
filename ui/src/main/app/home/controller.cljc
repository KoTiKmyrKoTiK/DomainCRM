(ns app.home.controller
  (:require [re-frame.core :refer [reg-event-fx]]

            [common.routes :as cr]))

(reg-event-fx
 cr/pid-dashboard
 (fn [_ _]
   {}))