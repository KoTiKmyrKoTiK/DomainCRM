(ns app.domains.model
  (:require [clojure.string :as str]

            [re-frame.core :refer [reg-sub]]

            [app.helpers :as h]
            [app.pages :as pages]

            [zframes.menu :as menu]

            [app.domains.crud.form :as form]

            [common.utils.date :as cud]
            [common.routes.domains :as pid]))

(def reg-domains-page (pages/reg-page-fn pid/search))

(defn role-item
  [r class]
  [:p
   (cond-> {:class (-> "rounded-md whitespace-nowrap mt-0.5 px-1.5 py-0.5 text-xs font-medium ring-1 ring-inset"
                       (str/split #" "))}
     (seq class)
     (update :class concat class))
   r])

(def status-map
  (->> form/status-items
       (reduce
        (fn [acc {:keys [value display]}]
          (cond-> acc
            value
            (assoc value display)))
        {})))

(defn status-item
  [r class]
  [:p
   (cond-> {:class (-> "rounded-md whitespace-nowrap mt-0.5 px-1.5 py-0.5 text-xs font-medium ring-1 ring-inset"
                       (str/split #" "))}
     (seq class)
     (update :class concat class))
   r])

(defn format-item
  [{:keys [id domain subdomain server_ip status 
           domain_server_provider domain_provider user
           created_at]}]
  {:id              id
   :href            (menu/href "domains" id)
   :domain          (cond->> domain
                      subdomain
                      (str subdomain "."))
   :ip              server_ip
   :server_provider (:name domain_server_provider)
   :provider        (:name domain_provider)
   :user            (when user (str (:name user) " (" (:email user) ")"))
   :created_at      (cud/ru-datetime-short created_at)
   :status          (case status
                      "new"        [status-item (get status-map status) ["text-yellow-700" "bg-yellow-50" "ring-yellow-600/20"]]
                      "active"     [status-item (get status-map status) ["text-green-700" "bg-green-50" "ring-green-600/20"]]
                      "reserved"   [status-item (get status-map status) ["text-purple-700" "bg-purple-50" "ring-purple-600/20"]]
                      "archived"   [status-item (get status-map status) ["text-gray-700" "bg-gray-50" "ring-gray-600/20"]]
                      "restricted" [status-item (get status-map status) ["text-red-700" "bg-red-50" "ring-red-600/20"]]
                      nil)})

(reg-sub
 pid/search
 :<- [:xhr/response pid/search]
 (fn [{data :data} _]
   {:items       (->> data 
                      (map format-item)
                      (group-by :server_provider))
    :create-href (menu/href "domains" "create")}))