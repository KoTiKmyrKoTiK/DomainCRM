(ns app.domains.model
  (:require [clojure.string :as str]

            [re-frame.core :refer [reg-sub]]

            [app.helpers :as h]
            [app.pages :as pages]

            [zframes.menu :as menu]

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

(defn format-item
  [{:keys [id name email role]}]
  {:id    id
   :href  (menu/href "domains" id)
   :name  name
   :email email
   :role  (case role
            "admin"  [role-item "Администратор" ["text-blue-700" "bg-blue-50" "ring-blue-600/20"]]
            "buyer"  [role-item "Байер"         ["text-green-700" "bg-green-50" "ring-green-600/20"]]
            "farmer" [role-item "Фармер"        ["text-yellow-700" "bg-yellow-50" "ring-yellow-600/20"]]
            [role-item "-" ["text-green-700" "bg-gray-50" "ring-gray-600/20"]])})

(reg-sub
 pid/search
 :<- [:xhr/response pid/search]
 (fn [{data :data} _]
   {:items       (map format-item data)
    :create-href (menu/href "domains" "create")}))