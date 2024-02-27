(ns app.domain-providers.model
  (:require [clojure.string :as str]

            [re-frame.core :refer [reg-sub]]

            [app.helpers :as h]
            [app.pages :as pages]

            [zframes.menu :as menu]

            [common.routes.domain-providers :as pid]))

(def reg-domain-providers-page (pages/reg-page-fn pid/search))

(defn format-item
  [{:keys [id name]}]
  {:id   id
   :href (menu/href "domain_providers" id)
   :name name})

(reg-sub
 pid/search
 :<- [:xhr/response pid/search]
 (fn [{data :data} _]
   {:items       (map format-item data)
    :create-href (menu/href "domain_providers" "create")}))