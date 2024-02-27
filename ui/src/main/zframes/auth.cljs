(ns zframes.auth
  (:require [re-frame.core  :as rf]
            [common.routes  :as routes]

            [clojure.string :as str]
            
            [zframes.redirect :as r]
            [zframes.menu     :as menu]
            [zframes.routing]))

(rf/reg-event-fx
 ::resolve-page
 (fn [{db :db} _]
   (let [location (:location/search db)
         search-params* (select-keys location [:u :r])
         {:keys [r]}  (zframes.routing/parse-search-params search-params*)

         m (->> r
                (menu/get-available-items _)
                first)]
     {:zframes.redirect/redirect {:uri (:href m)}})))

(rf/reg-event-fx
 ::signin-success
 (fn [{db :db} [_ data]]
   {:fx [[:storage/set {:u (-> data :user :id)}]
         [:dispatch [::userinfo]]
         [:db (assoc db :route-map/routes routes/routes)]
         [:route-map/start {}]
         [:zframes.redirect/page-redirect
          (->> data :user :role
               (menu/get-available-items _)
               first :href
               (hash-map :uri))]]}))

(rf/reg-event-fx
 ::authorize
 (fn [{db :db} [_ location]]
   (let [{:keys [hash-route]} location]
     (merge {:db              (assoc db :route-map/routes (select-keys routes/routes [:. "login"]))
             :route-map/start {}}
            (when-not (#{"/login"} hash-route)
              {:zframes.redirect/redirect {:uri "/login"}})))))

(rf/reg-event-fx
 ::userinfo
 [(rf/inject-cofx :storage/get [:u])]
 (fn [{s :storage} _]
   (let [id (:u s)]
     {:xhr/fetch [{:uri     (str "/api/users/" id)
                   :req-id  ::userinfo
                   :success {:event ::userinfo-success}}]})))

(rf/reg-event-fx
 ::userinfo-success
 [(rf/inject-cofx :storage/get [:r])
  (rf/inject-cofx :window-location)]
 (fn [{{storage-role :role} :storage location :location db :db} [_ {user :data}]]
   (let [{:keys [r u]} (get-in location [:query-string])
         role    (cond-> r
                   (not= r (:role user))
                   ((constantly (:role user))))

         user-id (:id user)

         hash-l  (-> js/window .-location .-hash)]
     (cond-> {:fx [[:db (assoc db ::userinfo user)]
                   [:route-map/start {}]
                   [::r/set-query-string {:r role :u user-id}]]}
       (nil? storage-role)
       (update :fx conj [:storage/set {:r role}])

       (str/blank? hash-l)
       (update :fx conj [:dispatch [::resolve-page]])))))

(rf/reg-sub
 ::userinfo
 (fn [db _] (::userinfo db)))

(rf/reg-sub
 :auth/userinfo
 (fn [db _] (::userinfo db)))

(rf/reg-event-fx
 ::logout
 (fn [& _]
   {:storage/remove [:r :u]
    :zframes.redirect/page-redirect {:uri "/"}}))