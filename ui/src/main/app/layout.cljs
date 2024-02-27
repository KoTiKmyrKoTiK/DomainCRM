(ns app.layout
  (:require [re-frame.core :refer [reg-sub reg-event-fx subscribe]]
            
            [clojure.string :as str]

            [zframes.flash]
            [zframes.auth :as auth]
            [zframes.menu :as menu]
            [zframes.routing]

            ["@headlessui/react" :as h-ui-react]
            [reagent.core :as r]
            [headlessui-reagent.core :as h-ui]

            ["@heroicons/react/24/solid" :as hi-solid]))

(def h-ui-transition-root (r/adapt-react-class h-ui-react/Transition.Root))

(defn keep-menu
  [r menu fragment items]
  (keep
   #(cond-> %
      (#{(:id %)} menu)
      (assoc :current? true)

      (and (some-> fragment (str/includes? (:href %))) (not menu))
      (assoc :current? true))
   items))

(reg-sub
 ::navigation
 :<- [:route-map/location-search]
 :<- [:route-map/fragment]
 :<- [::menu/menu]
 (fn [[location fragment menu]]
   (let [{:keys [r]} (select-keys location [:u :r])
         role-pages  (get menu/pages-for-roles r #{})]
     {:title "На главную"
      :menu  (->> menu/map-menus
                  (filter (comp role-pages :id menu/get-main-subitem))
                  (keep-menu r menu fragment))})))

(defn nav-bar
  [& _]
  (let [!navigation (subscribe [::navigation])
        !user-email (subscribe [:db/get [::auth/userinfo :email]])]
    (fn [& [{:keys [mobile?]}]]
      (let [{:keys [menu]} @!navigation
            user-email     @!user-email

            main-menu
            (remove :category menu)

            menu-with-categories
            (filter :category menu)

            grouped-categories
            (group-by :category menu-with-categories)]
        [:nav {:class "flex flex-1 flex-col"}
         [:ul {:role  "list"
               :class "flex flex-1 flex-col gap-y-7"}
          [:li
           [:ul {:role  "list"
                 :class "-mx-2 space-y-1"}
            [:<>
             (for
              [i main-menu] ^{:key (str "mob-" (:id i))}
              [:li
               [:a
                (cond-> {:href (:href i)
                         :class (-> "group flex gap-x-3 rounded-md p-2 text-sm leading-6 font-semibold"
                                    (str/split #" "))}
                  (:current? i)
                  (update :class concat ["bg-gray-50" "text-brazz-600"])

                  (not (:current? i))
                  (update :class concat ["text-gray-700" "hover:text-brazz-600" "hover:bg-gray-50"]))
                [:> (:icon i)
                 (cond-> {:class (-> "h-6 w-6 shrink-0"
                                     (str/split #" "))}

                   (:current? i)
                   (update :class concat ["text-brazz-600"])

                   (not (:current? i))
                   (update :class concat ["text-gray-400" "group-hover:text-brazz-600"]))]
                (:name i)]])]]]
          (when (seq menu-with-categories)
            [:<>
             (for [[category category-menu] grouped-categories]
               ^{:key category}
               [:li
                [:div {:class "text-xs font-semibold leading-6 text-gray-400"} category]
                [:ul {:role  "list"
                      :class "-mx-2 mt-2 space-y-1"}
                 (for [i category-menu]
                   ^{:key (str category "-" (:id i))}
                   [:li
                    [:a
                     (cond-> {:href (:href i)
                              :class (-> "group flex gap-x-3 rounded-md p-2 text-sm leading-6 font-semibold"
                                         (str/split #" "))}
                       (:current? i)
                       (update :class concat ["bg-gray-50" "text-brazz-600"])

                       (not (:current? i))
                       (update :class concat ["text-gray-700" "hover:text-brazz-600" "hover:bg-gray-50"]))
                     [:> (:icon i)
                      (cond-> {:class (-> "h-6 w-6 shrink-0"
                                          (str/split #" "))}

                        (:current? i)
                        (update :class concat ["text-brazz-600"])

                        (not (:current? i))
                        (update :class concat ["text-gray-400" "group-hover:text-brazz-600"]))]
                     (:name i)]])]])])
          (when-not mobile?
            [:li
             [:div {:class "text-xs font-semibold leading-6 text-gray-400"} "Профиль"]
             [:ul {:role  "list"
                   :class "-mx-2 mt-2 space-y-1"}
              [:li
               [:a {:href (menu/href "logout")
                    :class "text-gray-700 hover:text-brazz-600 hover:bg-gray-50 group flex gap-x-3 rounded-md p-2 text-sm leading-6 font-semibold"}
                [:span {:class "text-gray-400 border-gray-200 group-hover:border-brazz-600 group-hover:text-brazz-600 flex h-6 w-6 shrink-0 items-center justify-center rounded-lg border text-[0.625rem] font-medium bg-white"}
                 "X"]
                "Выйти из системы"]]]])
          (when-not mobile?
            [:li {:class "-mx-6 mt-auto"}
             [:a {:class "flex items-center gap-x-4 px-6 py-3 text-sm font-semibold leading-6 text-gray-900 hover:bg-gray-50"}
              [:div
               {:class "h-8 w-8 rounded-full bg-gray-50"}
               [:> hi-solid/UserIcon {:class "h-8 w-8"}]]
              [:span.sr-only "Your Profile"]
              [:span user-email]]])]]))))

(defn layout
  {:arglists '([cnt])}
  [_]
  (let [!navbar?    (subscribe [:db/get [:route-map/current-route :navbar?]])
        !title      (subscribe [:db/get [:route-map/current-route :title]])
        !user-email (subscribe [:db/get [::auth/userinfo :email]])

        !sidebarOpen? (r/atom false)]
    (fn layout
      [cnt]
      (let [navbar?    @!navbar?
            title      @!title
            user-email @!user-email
            
            sidebarOpen? @!sidebarOpen?]
        [:<>
         [:div.h.full
          (when (true? navbar?)
            [:<>
             [h-ui-transition-root
              {:show sidebarOpen?}
              [h-ui/dialog
               {:class    "relative z-50 lg:hidden"
                :on-close #(reset! !sidebarOpen? false)}
               [h-ui/transition-child
                {:class "fixed inset-0 bg-gray-900/80"}
                [h-ui/transition-child
                 {:enter      "transition ease-in-out duration-300 transform"
                  :enter-from "-translate-x-full"
                  :enter-to   "translate-x-0"
                  :leave      "transition ease-in-out duration-300 transform"
                  :leave-from "translate-x-0"
                  :leave-to   "-translate-x-full"
                  :class      "fixed inset-0 flex"}
                 [h-ui/dialog-panel {:class "relative mr-16 flex w-full max-w-xs flex-1"}
                  [h-ui/transition-child
                   {:class      "absolute left-full top-0 flex w-16 justify-center pt-5"
                    :enter      "ease-in-out duration-300"
                    :enter-from "opacity-0"
                    :enter-to   "opacity-100"
                    :leave      "ease-in-out duration-300"
                    :leave-from "opacity-100"
                    :leave-to   "opacity-0"}
                   [:button
                    {:class    "-m-2.5 p-2.5"
                     :on-click #(reset! !sidebarOpen? false)}
                    [:span
                     {:class "sr-only"}]
                    [:> hi-solid/XMarkIcon
                     {:class "h-6 w-6 text-white"}]]]
                  [:div {:class "flex grow flex-col gap-y-5 overflow-y-auto bg-white px-6 pb-2"}
                   [:div {:class "flex h-16 shrink-0 items-center"}
                    [:img
                     {:class "h-8 w-auto"
                      :src   "/img/logo.png"}]]
                   [nav-bar {:mobile? true}]]]]]]]

             [:div {:class "hidden lg:fixed lg:inset-y-0 lg:z-50 lg:flex lg:w-72 lg:flex-col"}
              [:div {:class "flex grow flex-col gap-y-5 overflow-y-auto border-r border-gray-200 bg-white px-6"}
               [:div {:class "flex h-16 shrink-0 items-center"}
                [:img {:class "h-8 w-auto" :src "/img/logo.png"}]]
               [nav-bar]]]

             [:div {:class "sticky top-0 z-40 flex items-center gap-x-6 bg-white px-4 py-4 shadow-sm sm:px-6 lg:hidden"}
              [:button {:class "-m-2.5 p-2.5 text-gray-700 lg:hidden"
                        :on-click #(reset! !sidebarOpen? true)}
               [:span.sr-only "Open sidebar"]
               [:> hi-solid/Bars3Icon {:class "h-6 w-6"}]]
              [:div {:class "flex-1 text-sm font-semibold leading-6 text-gray-900"}
               (cond-> "BrazzTeam CRM"
                 title
                 (str " - " title))]
              [h-ui/menu
               {:class "relative inline-block text-left"
                :as :div}
               [h-ui/menu-button
                [:div
                 {:class "h-8 w-8 rounded-full bg-gray-50"}
                 [:> hi-solid/UserIcon {:class "h-8 w-8"}]]]
               [h-ui/transition
                {:class      "absolute right-0 z-10 mt-2 w-56 origin-top-right divide-y divide-gray-100 rounded-md bg-white shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none"
                 :enter      "transition ease-out duration-100"
                 :enter-from "transform opacity-0 scale-95"
                 :enter-to   "transform opacity-100 scale-100"
                 :leave      "transition ease-in duration-75"
                 :leave-from "transform opacity-100 scale-100"
                 :leave-to   "transform opacity-0 scale-95"}
                [:div {:class "px-4 py-3"}
                 [:span.text-sm "Вы вошли как: "]
                 [:span {:class "truncate text-sm font-medium text-gray-900"}
                  user-email]]
                [:div.py-1
                 [h-ui/menu-item
                  (fn [{:keys [active]}]
                    [:a
                     (cond->  {:class (-> "block px-4 py-2 text-sm text-gray-700" (str/split #" "))
                               :href (menu/href "logout")}
                       active
                       (update :class concat ["bg-gray-100" "text-gray-900"])
                       
                       (not active)
                       (update :class concat ["text-gray-700"]))
                     "Выйти из аккаунта"])]]]]]])
          [:main.h-full
           (cond-> {:class [:py-10]}
             navbar?
             (update :class conj :lg:pl-72))
           [:div {:class "mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 h-full"}
            [:div {:class "mx-auto max-w-3xl"}
             cnt]]]]
         [zframes.flash/flashes]]))))