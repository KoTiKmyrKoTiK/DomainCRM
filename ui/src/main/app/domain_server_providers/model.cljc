(ns app.domain-server-providers.model
  (:require [clojure.string :as str]

            [re-frame.core :refer [reg-sub]]

            [app.helpers :as h]
            [app.pages :as pages]

            [zframes.menu :as menu]

            [common.routes.domain-server-providers :as pid]))

(def reg-domain-server-providers-page (pages/reg-page-fn pid/search))

(defn format-item
  [{:keys [id name]}]
  {:id   id
   :href (menu/href "domain_server_providers" id)
   :name name})

(reg-sub
 pid/search
 :<- [:xhr/response pid/search]
 (fn [{data :data} _]
   {:items       (map format-item data)
    :create-href (menu/href "domain_server_providers" "create")}))