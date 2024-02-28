(ns app.users.model
  (:require [clojure.string :as str]

            [re-frame.core :refer [reg-sub]]

            [app.helpers :as h]
            [app.pages :as pages]

            [zframes.menu :as menu]

            [common.routes.users :as pid]))

(def reg-users-page (pages/reg-page-fn pid/search))

(defn badge-item
  [r class]
  [:p
   (cond-> {:class (-> "rounded-md whitespace-nowrap mt-0.5 px-1.5 py-0.5 text-xs font-medium ring-1 ring-inset"
                       (str/split #" "))}
     (seq class)
     (update :class concat class))
   r])

(defn format-item
  [{:keys [id name email role status]}]
  {:id     id
   :href   (menu/href "users" id)
   :name   name
   :email  email
   :badges
   (cond-> [:<> (case role
                  "admin"     [badge-item "Администратор" ["text-blue-700" "bg-blue-50" "ring-blue-600/20"]]
                  "buyer"     [badge-item "Байер"         ["text-green-700" "bg-green-50" "ring-green-600/20"]]
                  "team_lead" [badge-item "Тимлид"        ["text-purple-700" "bg-purple-50" "ring-purple-600/20"]]
                  "farmer"    [badge-item "Фармер"        ["text-yellow-700" "bg-yellow-50" "ring-yellow-600/20"]]
                  [badge-item "-" ["text-gray-700" "bg-gray-50" "ring-gray-600/20"]])]
     (#{"restricted"} status)
     (conj [badge-item "Заблокирован" ["text-red-700" "bg-red-50" "ring-red-600/20"]]))})

(reg-sub
 pid/search
 :<- [:xhr/response pid/search]
 (fn [{data :data} _]
   {:items       (map format-item data)
    :create-href (menu/href "users" "create")}))