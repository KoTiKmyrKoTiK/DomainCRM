(ns common.routes
  (:require 
   [common.routes.login  :as pid-login]
   [common.routes.common :as pid-common]
   [common.routes.users  :as pid-users]))

(def routes
  (merge
   pid-login/routes
   pid-common/routes
   pid-users/routes
   {:.       pid-login/root
    "logout" {:. :zframes.auth/logout}
    "home"   {:.     pid-common/dashboard
              :title "Dashboard"}}))

(defn route-index*
  [route pth]
  (merge
   (hash-map (str (:. route))
             (assoc route :pth pth))
   (reduce-kv
    (fn [acc k v]
      (if (or (string? k) (vector? k))
        (merge acc (route-index* v (into pth [k "/"])))
        acc))
    {}
    route)))

(def route-index
  (route-index* routes ["/"]))
