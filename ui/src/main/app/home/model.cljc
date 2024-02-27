(ns app.home.model
  (:require [clojure.string :as str]

            [re-frame.core :refer [reg-sub]]

            [app.helpers :as h]
            [app.pages :as pages]

            [common.routes.common :as pid]))

(def reg-dashboard-page (pages/reg-page-fn pid/dashboard))

(reg-sub
 pid/dashboard
 (fn [db] {}))