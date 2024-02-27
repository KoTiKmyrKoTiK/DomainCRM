(ns app.users.crud.model
  (:require [clojure.string :as str]

            [re-frame.core :refer [reg-sub]]

            [app.helpers :as h]
            [app.pages :as pages]

            [zframes.menu :as menu]

            [common.routes.users :as pid]))

(def reg-create-page (pages/reg-page-fn pid/create))
(def reg-edit-page   (pages/reg-page-fn pid/edit))

(reg-sub
 pid/create
 :<- [:db/get [:route-map/current-route :title]]
 (fn [title _]
   {:type  :create
    :title title}))

(reg-sub
 pid/edit
 :<- [:xhr/response pid/edit]
 :<- [:db/get [:route-map/current-route :title]]
 (fn [[{:keys [data]} title] _]
   {:type  :edit
    :title title}))