(ns app.core
  (:require [reagent.core  :as r]
            [re-frame.core :as rf]

            [zframes.debounce]
            [zframes.routing :as routing]
            [zframes.redirect]
            [zframes.xhr]
            [zframes.storage]
            [zframes.auth :as auth]
            [zframes.window-location]
            [zframes.events]
            [zframes.effects]

            [app.layout]
            [app.pages]

            [app.login.view]
            [app.home.view]
            [app.users.view]
            [app.domains.view]

            [common.routes]

            [goog.dom :as gdom]

            ["react-dom/client" :refer [createRoot]]))

(def default-config
  {:base-url "http://localhost:8078"})

(defn ui-config
  [_]
  (merge
   default-config
   (when-let [c (aget js/window "__APP_SETTINGS ")]
     (js->clj c :keywordize-keys true))))

(rf/reg-sub
 :ui/config
 (fn [db _]
   (:config db)))

(rf/reg-event-fx
 ::initialize
 [(rf/inject-cofx :storage/get [:u])
  (rf/inject-cofx :window-location)]
 (fn [{storage :storage location :location db :db} _]
   (let [config          (ui-config location)
         auth            (:u storage)
         db              (-> db
                             (merge {:config config
                                     :theme  (:theme storage)})
                             (assoc-in [:xhr :config] config))]
     (merge
      {:db db}
      (if auth
        {:db (->> (dissoc common.routes/routes "login")
                  (routing/assoc-routes db))
         :fx [[:dispatch [::auth/userinfo]]]}
        {:dispatch [::auth/authorize location]})))))

(defn not-found-page-html
  []
  [:main
   {:class
    "grid min-h-full place-items-center bg-white px-6 py-24 sm:py-32 lg:px-8"}
   [:div
    {:class "text-center"}
    [:p {:class "text-base font-semibold text-brazz-600"} "404"]
    [:h1
     {:class
      "mt-4 text-3xl font-bold tracking-tight text-gray-900 sm:text-5xl"}
     "Страница не найдена"]
    [:p
     {:class "mt-6 text-base leading-7 text-gray-600"}
     "Извините, мы не смогли найти нужную вам страницу."]
    [:div
     {:class "mt-10 flex items-center justify-center gap-x-6"}
     [:button
      {:on-click #(rf/dispatch [:zframes.redirect/redirect {:uri "/"}]),
       :class
       "rounded-md bg-brazz-600 px-3.5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brazz-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brazz-600"}
      "Домой"]]]])

(defn- not-found-page
  [_]
  (let [!loading? (r/atom true)]
    (r/create-class
     {:component-did-mount
      (fn [_]
        (js/setTimeout (fn [] (reset! !loading? false)) 2000))

      :reagent-render
      (fn
        [route]
        (if (true? @!loading?)
          [:div#not-found
           (cond-> {:style {:position "fixed"
                            :top "50%"
                            :width "100%"
                            :text-align "center"}}
             (true? (:navbar? @route))
             (assoc :class ["lg:left-36" "sm:left-0"])
             
             (not (true? (:navbar? @route)))
             (assoc :class ["left-0"]))
           [:div.indicator
            [:svg {:width "16px" :height "12px"}
             [:polyline#back {:points "1 6 4 6 6 11 10 1 12 6 15 6"}]
             [:polyline#front {:points "1 6 4 6 6 11 10 1 12 6 15 6"}]]]]
          [not-found-page-html]))})))

(defn- error-page
  {:arglists '([error])}
  [_]
  [:<>])

(defn- current-page
  []
  (let [route  (rf/subscribe [:route-map/current-route])
        state  (r/atom {:error nil})
        error  (r/track #(:error @state))]
    (r/create-class
     {:get-derived-state-from-error
      (fn [err]
        (swap! state assoc :error err))

      :component-did-catch
      (fn [_ _ _]
        (rf/dispatch [:flash/error {:msg [:div
                                           [:div "Произошла ошибка…"]]}]))

      :reagent-render
      (fn current-page
        []
        (let [page (get @app.pages/pages (:match @route))
              params (:params @route)]
          [app.layout/layout
           (cond
             @error
             [error-page @error]

             page
             [page params]

             :else
             [not-found-page route])]))})))

(defonce root (createRoot (gdom/getElement "app")))

(defn- mount-root
  []
  (js/addEventListener "submit" (fn [e] (.preventDefault e)))
  (. root render (r/as-element [current-page])))

(defn ^:export initialize!
  []
  (enable-console-print!)

  (rf/dispatch-sync [::initialize])
  (mount-root))