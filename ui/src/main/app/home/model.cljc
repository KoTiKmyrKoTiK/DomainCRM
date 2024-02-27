(ns app.home.model
  (:require [clojure.string :as str]

            [re-frame.core :refer [reg-sub]]

            [app.helpers :as h]
            [app.pages :as pages]

            [common.routes :as cr]))

(def reg-dashboard-page (pages/reg-page-fn cr/pid-dashboard))

(reg-sub
 cr/pid-dashboard
 (fn [db] {}))