(ns app.domains.model
  (:require [clojure.string :as str]

            [re-frame.core :refer [reg-sub]]

            [app.helpers :as h]
            [app.pages :as pages]

            [zframes.menu :as menu]
            [zframes.auth :as auth]

            [app.domains.crud.form :as crud-form]
            [app.domains.form      :as form]

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
  (->> crud-form/status-items
       (reduce
        (fn [acc {:keys [value display]}]
          (cond-> acc
            value
            (assoc value display)))
        {})))

(def group-by-map
  (->> form/group-items
       (reduce
        (fn [acc {:keys [value label]}]
          (cond-> acc
            value
            (assoc value label)))
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

(defn make-filters
  [params]
  (reduce-kv
   (fn [acc k v]
     (let [vals-set (-> (or v "")
                        (str/split #",")
                        set)

           f (case k
               :user
               (comp vals-set :id :user)

               :domain-provider
               (comp vals-set :id :domain_provider)

               :domain-server-provider
               (comp vals-set :id :domain_server_provider)

               nil)]
       (cond-> acc
         f (conj f))))
   []
   params))

(reg-sub
 pid/search
 :<- [:xhr/response pid/search]
 :<- [::auth/userinfo]
 :<- [:route-map/fragment-params]
 (fn [[{data :data} {:keys [role]} {:keys [params]}] _]
   (let [group-fn (if (:group-by params)
                    (fn [item]
                      (let [k (:group-by params)
                            kw (keyword k)
                            v (kw item)]
                        [:span
                         [:span.text-gray-500.font-normal (get group-by-map k) ": "]
                         [:span v]]))
                    (constantly [:span.text-gray-500.font-normal "Без группировки"]))
         
         filter-preds (make-filters params)]
     {:grouped-items (cond-> data
                       (seq filter-preds)
                       (->> (filter (apply every-pred filter-preds)))

                       :always
                       (->> (map format-item)
                            (sort-by :domain)
                            (group-by group-fn)))
      :role          role
      :create-href   (menu/href "domains" "create")})))