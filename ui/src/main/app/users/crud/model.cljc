(ns app.users.crud.model
  (:require [clojure.string :as str]

            [re-frame.core :refer [reg-sub]]

            [app.helpers :as h]
            [app.pages :as pages]

            [zframes.menu :as menu]

            [common.routes :as cr]))

(def reg-create-page (pages/reg-page-fn cr/pid-users-create))
(def reg-edit-page   (pages/reg-page-fn cr/pid-users-edit))

(reg-sub
 cr/pid-users-create
 :<- [:db/get [:route-map/current-route :title]]
 (fn [title _]
   {:type  :create
    :title title}))

(reg-sub
 cr/pid-users-edit
 :<- [:xhr/response cr/pid-users-edit]
 :<- [:db/get [:route-map/current-route :title]]
 (fn [[{:keys [data]} title] _]
   {:type  :edit
    :title title}))