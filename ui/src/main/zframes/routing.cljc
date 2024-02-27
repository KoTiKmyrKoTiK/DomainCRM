(ns zframes.routing
  (:require [clojure.string :as str]

            [re-frame.db   :as db]
            [re-frame.core :as rf]

            [zenform.model :as zf]

            [zframes.menu            :as menu]
            [zframes.window-location :as window-location]

            [route-map.core :as route-map]

            [common.utils :as cu]
            
            [goog.crypt.base64 :as b64]))

(defn parse-search-params
  [search]
  (reduce-kv
   (fn [acc k v]
     (assoc acc k
            (->> (b64/decodeString v))))
   {} search))

(defn gen-uuid
  []
  (str #?(:clj (java.util.UUID/randomUUID)
          :cljs (random-uuid))))

#?(:clj (defonce window-location (atom {:search ""
                                        :hash ""
                                        :href ""})))

(defn dispatch-routes
  [& _]
  (let [fragment #?(:cljs (.. js/window -location -hash)
                    :clj (:hash @window-location))]
    (rf/dispatch [:fragment-changed fragment])))

;; Subs
;; ---------

(rf/reg-sub
 :route-map/group
 (fn [db _]
   (get-in db [:fragment-params :search :group])))

(rf/reg-sub
 :route-map/fragment-params
 (fn [db _]
   (:fragment-params db)))

(rf/reg-sub
 :route-map/fragment
 (fn [db _]
   (:fragment db)))

(rf/reg-sub
 :route-map/current-route
 (fn [db _]
   (:route-map/current-route db)))

(rf/reg-sub
 :route-map/current-idx
 (fn [db _]
   (get-in db [:route-map/current-route :match])))

(rf/reg-sub
 :route-map/fragment-path
 (fn [db _]
   (:fragment-path db)))

(rf/reg-sub
 :route-map/location-search
 (fn [db _]
   (parse-search-params (:location/search db))))

;; Cofx
;; ---------

(rf/reg-cofx
 :route-map/fragment-params
 (fn [{:keys [db] :as cofx} _]
   (->> (:fragment-params db)
        (assoc cofx :route-map/fragment-params))))

(rf/reg-cofx
 :route-map/fragment
 (fn [{:keys [db] :as cofx} _]
   (->> (:fragment db)
        (assoc cofx :route-map/fragment))))

(rf/reg-cofx
 :route-map/current-route
 (fn [{:keys [db] :as cofx} _]
   (->> (:route-map/current-route db)
        (assoc cofx :route-map/current-route))))

(rf/reg-cofx
 :route-map/current-idx
 (fn [{:keys [db] :as cofx} _]
   (->> (get-in db [:route-map/current-route :match])
        (assoc cofx :route-map/current-idx))))

(rf/reg-cofx
 :route-map/fragment-path
 (fn [{:keys [db] :as cofx} _]
   (->> (:fragment-path db)
        (assoc cofx :route-map/fragment-path))))

(rf/reg-cofx
 :route-map/location-search
 (fn [{:keys [db] :as cofx} _]
   (->> (:location/search db)
        (assoc cofx :route-map/location-search))))

;; ---------

(rf/reg-fx
 :html-title
 #?(:cljs (fn [title]
            (set! (.-title  js/document) (str  "BrazzTeam CRM" (when title (str " - " title)))))
    :clj  (constantly nil)))

(defn contexts-diff
  [current-contexts old-contexts phases]
  (let [deinit?          (some #{:deinit} phases)
        init?            (some #{:init} phases)
        page-in-phases   (vec (remove #{:deinit} phases))
        old-contexts-evs (reduce (fn [acc [ctx-id params]]
                                   (cond-> acc
                                     deinit?
                                     (conj [ctx-id :deinit (assoc params
                                                                  :phases [:deinit])])))
                                 [] old-contexts)
        new-contexts-evs (reduce (fn [acc [ctx-id params]]
                                   (cond-> acc
                                     init?
                                     (conj [ctx-id :init   (assoc params
                                                                  :phases page-in-phases)])))
                                 [] current-contexts)]
    (into old-contexts-evs new-contexts-evs)))

(defn parse-params
  [s]
  (if (str/blank? s)
    {}
    (reduce
     (fn [acc pair]
       (let [[k v] (str/split pair #"=" 2)]
         (assoc acc (keyword k) #?(:cljs (js/decodeURIComponent v)
                                   :clj v))))
     {} (-> (str/replace s #"^\?" "")
            (str/split #"\&")))))

(defn dispatch-context
  [_]
  (let [query #?(:cljs (.. js/window -location -search)
                 :clj (:search @window-location))]
    (rf/dispatch [:search-changed (parse-params query)])))

(rf/reg-event-fx
 :search-changed
 (fn [{db :db} [_ search]]
   (let [old     (:location/search db)
         ctx-rs  (:context/routes db)
         prev-hs (get ctx-rs (:contexts old))]
     (if (not (= old search))
       (do
         (when (seq prev-hs)
           {:fx (mapv (fn [prev-h] [:dispatch [prev-h :deinit old]]) prev-hs)
            :db (dissoc db :navigation)})
         (if-let [handlers (get ctx-rs (:contexts search))]
           {:fx (mapv (fn [handler] [:dispatch [handler :init search]]) handlers)
            :db (assoc db :location/search search)}
           {:db (assoc db
                       :location/search search)}))
       {:db (assoc db :location/search search)}))))

(defn parse-fragment
  [fragment]
  (let [[path params-str] (-> fragment
                              (str/replace #"^#" "")
                              (str/split #"\?"))
        params  (if (str/blank? params-str) {} (window-location/parse-query-string (or params-str "")))]
    {:path path
     :query-string params-str
     :params params}))

(rf/reg-event-fx
 :fragment-changed
 [zf/before-transition]
 (fn [{:zf/keys [possible-data-loss?] {phases ::phases :as db} :db} [_ fragment]]
   (let [{path :path q-params :params qs :query-string} (parse-fragment fragment)]
     (when (and (seq path) (or (not= fragment (:fragment db))
                               (not= (window-location/parse-query-string
                                      #?(:clj  (:search window-location)
                                         :cljs (aget js/window "location" "search")))
                                     (get-in db [:fragment-params :search]))))
       (let [route
             (route-map/match [:. path] (:route-map/routes db))]
         (cond
           possible-data-loss?
           (do
             #?(:clj  (swap! window-location assoc :hash (:fragment-path db))
                :cljs (aset js/window "location" "hash" (:fragment-path db)))
             {:dispatch [:app.form/warn-data-loss (subs fragment 1)]})

           route
           (let [route-manifest
                 (-> route :parents (last))

                 x-corr-id
                 (gen-uuid)

                 params
                 (-> route :params
                     (assoc :params q-params)
                     (assoc :search (:location/search db)))

                 current-page
                 (:match route)

                 route-map
                 {:match       current-page
                  :path        path
                  :template    (:path route)
                  :parsed-path (route-map/pathify path)
                  :params      params
                  :navbar?     (if (some? (:navbar? route-manifest))
                                 (:navbar? route-manifest)
                                 true)
                  :title       (:title route-manifest)
                  :parents     (:parents route)}

                 redirect-events
                 (when-not (= path "/login")
                   (->> params :search
                        (parse-search-params)
                        :r
                        (menu/dispatch-redirect? (assoc db :fragment fragment))))

                 old-path
                 (get-in db [:route-map/current-route :path])

                 old-page
                 (get-in db [:route-map/current-route :match])

                 old-params
                 (get-in db [:route-map/current-route :params])

                 page-changed?
                 (not
                  (and (= current-page old-page)                                         ; same page
                       (= path old-path)                                                 ; same path  
                       (apply = (map :search [params old-params]))                       ; same role
                       (apply = (map #(dissoc % :search :params) [params old-params])))) ; same IDs

                 params?
                 (boolean
                  (or (seq (:params params))
                      (and (empty? (:params params))
                           (seq    (:params old-params))
                           (not    page-changed?))))

                 phases
                 (cond-> []
                   (and old-page
                        (or page-changed?
                            (some #{:deinit} phases)))
                   (conj :deinit)

                   (or (nil? old-page)
                       page-changed?
                       (some #{:init} phases))
                   (conj :init)

                   params?
                   (conj :params))

                 page-out-events
                 (cond-> []
                   (not= current-page old-page)
                   ((fn page-change-events
                      [evs]
                      (-> evs
                          (conj [::expand-toggle false])
                          ;; Close  modals
                          (into (reduce-kv (fn [acc _ modal]
                                             (cond-> acc
                                               (not (:page-persistent modal))
                                               (conj acc [:close-modal modal])))
                                           [] (:modal db))))))

                   (some #{:deinit} phases)
                   (into [[old-page :deinit old-params]
                          [::remove-page-unload {:fragment-changed? true}]]))

                 old-contexts
                 (:route/context db)

                 current-contexts
                 (when-not redirect-events
                   (->> route-map :parents
                        (mapcat (fn [{ctxs :contexts, index :.}]
                                  (map (fn [ctx] {ctx (assoc params :.. index, :. current-page)})
                                       ctxs)))
                        (into {})))

                 context-evs
                 (contexts-diff current-contexts old-contexts phases)

                 page-in-events
                 (if redirect-events
                   redirect-events
                   (cond-> []
                     (some #{:init} phases)
                     (conj [current-page :init params])

                     (some #{:params} phases)
                     (conj [current-page :params params old-params])))

                 evs
                 (->> (concat context-evs page-in-events)
                      (filterv identity))

                 before-evs
                 (->> route-map :parents
                      (mapcat (fn [{rs :before r :.}]
                                (map #(vector % {:route-params (assoc params :.. r :. current-page :from {:pid old-page :params old-params :path old-path})}) rs)))
                      (reverse)
                      (vec))

                 db' (cond-> db
                       page-changed?
                       (dissoc old-page
                               :dialog :form :button-spinner :zframes.breadcrumb/breadcrumb
                               :restriction :listen :page)

                       :always
                       (-> (assoc :route/history (conj (take 4 (:route/history db))
                                                       {:route  current-page
                                                        :uri    fragment
                                                        :search (:search params)})
                                  :hide-menu-nav false
                                  :fragment fragment
                                  :fragment-params params
                                  :fragment-path path
                                  :fragment-query-string qs
                                  :route/context current-contexts
                                  :route-map/current-route route-map)
                           (assoc-in [:xhr :config :x-correlation-id] x-corr-id)
                           (dissoc ::phases)))]
             (cond->
              {:db         db'
               :html-title (or (:title route-manifest) nil)
               :fx         (->> (if (and (seq before-evs) (seq page-in-events))
                                  (conj page-out-events [::before-evs before-evs evs {}])
                                  (into page-out-events evs))
                                (keep (fn [ev] (when (seq ev) [:dispatch ev]))))}

               (not= current-page old-page)
               #?(:clj  identity
                  :cljs (assoc :scroll/to-position [0]))

               (and (not= current-page old-page)
                    (not (:modal db))) ; If there is modals then delegate logic to modal handler, as they are similar.
               #?(:clj  identity
                  :cljs (assoc :app.components.dialog/toggle-body-scroll true))))

           :else
           {:db (assoc db
                       :hide-menu-nav           false
                       :fragment                fragment
                       :route-map/current-route nil
                       :route-map/current-route nil
                       :route-map/error         :not-found)}))))))

(rf/reg-event-fx
 ::before-evs
 (fn [_ [_ before-evs evs payload]]
   (let [[event data] (first before-evs)]
     {:dispatch [event {:data    (merge data payload)
                        :success {:event  ::continue-flow
                                  :params {:next-evs  (rest before-evs)
                                           :final-evs evs}}}]})))

(rf/reg-event-fx
 ::continue-flow
 (fn [_ [_ {:keys [next-evs final-evs data]}]]
   (if (seq next-evs)
     {:dispatch [::before-evs next-evs final-evs data]}
     {:fx (mapv #(vector :dispatch %) final-evs)})))

(rf/reg-event-db
 ::save-resource-by-pid
 (fn [db [_ {:keys [data]} {:keys [pid]}]]
   (assoc-in db [pid :resource] data)))

(rf/reg-event-db
 ::expand-toggle
 (fn [db [_ val]]
   (assoc-in db [::navigation :expand] val)))

(rf/reg-sub
 :pop-route
 (fn [db]
   (peek (:route-stack db))))

(rf/reg-event-db
 :clear-route-stack
 (fn [db _]
   (dissoc db :route-stack)))

(defn history-fx
  [_]
  #?(:cljs (aset js/window "onhashchange" dispatch-routes)
     :clj (swap! window-location assoc :hash ""))
  (dispatch-routes nil))

(rf/reg-fx :history history-fx)

(defn search-history-fx
  [_]
  #?(:cljs (aset js/window "onpopstate" dispatch-context)
     :clj (swap! window-location assoc :search ""))
  (dispatch-context nil))

(rf/reg-fx :search-history search-history-fx)

#?(:cljs
   (defn- page-unload-handler
     [^js/Event ev]
     (.preventDefault ev)
     ""))

(defn start-fx
  [_]
  (search-history-fx nil)
  (history-fx nil))

(defn check-needs-prevent-data-loss
  [form-path]
  (or (true? (->> @db/app-db
                  :route-map/current-route :parents last
                  :form/prevent-data-loss?))
      (true? (get-in @db/app-db (conj form-path :dialog?)))))

(zf/reg-checking-fn
 #?(:clj  (fn [_] nil)
    :cljs check-needs-prevent-data-loss))

(zf/reg-form-change-fx
 ::set-page-unload
 #?(:clj  (fn [_] nil)
    :cljs (fn [form-path]
            (when (and (nil? (aget js/window "onbeforeunload"))
                       (check-needs-prevent-data-loss form-path))
              (aset js/window "onbeforeunload" page-unload-handler)))))

(rf/reg-fx
 ::remove-page-unload
 (fn [_]
   #?(:clj  nil
      :cljs (when (some? (aget js/window "onbeforeunload"))
              (aset js/window "onbeforeunload" nil)))))

(rf/reg-event-fx
 ::remove-page-unload
 [zf/before-transition]
 (fn [{:zf/keys [possible-data-loss?]} [_ {:keys [fragment-changed?]}]]
   (when (or fragment-changed? (not possible-data-loss?))
     {::remove-page-unload nil})))

(rf/reg-fx :route-map/start start-fx)

(defn redirect
  [href]
  #?@(:cljs ((aset (.-location js/window) "hash" href))
      :clj  ((swap! window-location assoc :hash (if (str/blank? href)
                                                  ""
                                                  (str "#" href)))
             (dispatch-routes nil))))

(defn rredirect
  [href]
  (let [[_ search hash] (cond
                          (str/blank? href)        [href nil ""]
                          (str/includes? href "#") (re-find #"^(?:/\?(.+))?(\#.*)$" href)
                          :else                    [href nil (str "#" href)])]
    #?@(:cljs ((aset (.-location js/window) "hash" hash)
               (when-not (str/blank? search)
                 (aset (.-location js/window) "search" search)))
        :clj  ((when-not (str/blank? search)
                 (swap! window-location assoc :search search)
                 (dispatch-context nil))
               (swap! window-location assoc :hash hash)
               (dispatch-routes nil)))))

(rf/reg-fx
 :route-map/redirect
 redirect)

(rf/reg-event-fx
 :route-map/redirect
 (fn [_ [_ href]]
   {:route-map/redirect href}))

(rf/reg-fx
 :route-map/rredirect
 rredirect)

(rf/reg-event-fx
 :route-map/rredirect
 (fn [_ [_ href]]
   {:route-map/rredirect href}))

(defn to-query-params
  [params]
  (when (seq params)
    (->> params
         (cu/clear-map)
         (map (fn [[k v]]
                (str (name k) "=" (if ((some-fn symbol? keyword?) v)
                                    (name v)
                                    (str v)))))
         (str/join "&"))))

(defn to-hash
  [opts]
  (str "#" (:path opts) "?" (to-query-params (:params opts))))

(defn make-fragment
  [params]
  (let [opts  (parse-fragment #?(:cljs (.. js/window -location -hash)
                                 :clj (:hash @window-location)))]
    (to-hash (assoc opts :params params))))

(rf/reg-fx
 :route-map/set-params
 (fn [params]
   (let [opts  (parse-fragment #?(:cljs (.. js/window -location -hash)
                                  :clj (:hash @window-location)))
         value (to-hash (assoc opts :params params))]
     #?@(:cljs ((aset (.. js/window -location) "hash" value))
         :clj  ((swap! window-location assoc :hash (if (str/blank? value)
                                                     ""
                                                     (str "#" value)))
                (dispatch-routes nil))))))

(defn idx-routes
  ([routes path]
   (reduce-kv
    (fn [acc pth route]
      (if-let [event   (:. route)]
        (let [path    (conj path pth)]
          (merge
           (assoc acc event path)
           (idx-routes route path)))
        acc))
    {}
    routes))
  ([routes] (idx-routes routes ["#"])))

(defn assoc-routes
  [db routes]
  (assoc db :route-map/routes routes :route-map/idx-routes (idx-routes routes)))

(rf/reg-event-fx
 ::init
 (fn [{db :db} [_ routes]]
   {:db (assoc-routes db routes)
    :route-map/start {}}))

(defn template-route
  [route params]
  (map
   (fn [r]
     (if (string? r)
       r
       (get-in params r)))
   route))

(defn ev-short-href
  [ev & [{:keys [params] :as p}]]
  (let [route-params (dissoc p :location :search :params)
        routes       (get-in @db/app-db [:route-map/idx-routes])
        anchor-route (-> (get routes ev)
                         (template-route route-params)
                         (->> (str/join "/")))
        anchor-qs    (some->> params to-query-params (str \?))]
    (str anchor-route anchor-qs)))

(defn enrich-href
  ([href {:keys [location search params]}]
   (let [location  (or location (when search "/") "")
         search-qs (some->> search to-query-params (str \?))
         anchor-qs (some->> params to-query-params (str \?))]
     (str location search-qs href anchor-qs))))

(defn ev-href
  ([ev]
   (let [routes       (get-in @db/app-db [:route-map/idx-routes])
         anchor-route (->> (get routes ev)
                           (str/join "/"))]
     anchor-route))
  ([ev params]
   (let [route-params (dissoc params :location :search :params)
         routes       (get-in @db/app-db [:route-map/idx-routes])
         anchor-route (-> (get routes ev)
                          (template-route route-params)
                          (->> (str/join "/")))]
     (enrich-href anchor-route params))))

(rf/reg-event-fx
 :route-map/ev-href-redirect
 (fn [_ [_ & opts]]
   {:route-map/redirect (apply ev-href opts)}))

(defn replace-location
  {:arglists '([uri])}
  #?(:cljs [uri] :clj  [_])
  #?(:cljs (.replace js/location uri)))

(rf/reg-fx ::replace replace-location)

(rf/reg-event-fx
 ::replace
 (fn [_ [_ uri]]
   {::replace uri}))

(rf/reg-event-fx
 :route-map/replace-location
 (fn [_ [_ uri]]
   #?(:clj  {:dispatch [:zframes.redirect/redirect
                        {:uri (if (string? uri)
                                uri
                                (ev-short-href (:pid uri) (:params uri)))}]}
      :cljs {::replace (if (string? uri)
                         uri
                         (ev-href (:pid uri) (:params uri)))})))

(rf/reg-fx
 ::skip
 (fn [& _]
   nil))

(rf/reg-event-db
 ::force-phases
 (fn [db [_ phases]]
   (assoc db ::phases phases)))

(rf/reg-fx
 :history-back
 (fn [& _]
   #?(:cljs (.back js/history))))

(rf/reg-event-fx
 :history-back
 (fn [& _]
   {:history-back {}}))
