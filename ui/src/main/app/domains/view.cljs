(ns app.domains.view
  (:require [re-frame.core :refer [dispatch]]

            [zenform.core :as zf]

            [app.pages              :as pages]
            [app.form.inputs        :as inputs]
            [app.form.filter-inputs :as f-inputs]

            [app.components :as cmp]

            [app.domains.model      :as model]
            [app.domains.controller :as ctrl]
            [app.domains.form       :as form]

            [app.domains.crud.view]

            [headlessui-reagent.core :as h-ui]
            ["@heroicons/react/24/solid" :as hi-solid]))

(defn view
  [{:keys [grouped-items role create-href]} _]
  [:<>
   [cmp/title-divider "Домены"
    [:a {:href create-href
         :class "inline-flex items-center gap-x-1.5 rounded-full bg-white px-3 py-1.5 text-sm 
                    font-semibold text-gray-900 shadow-sm 
                    ring-1 ring-inset ring-gray-300 hover:bg-gray-50"}
     [:> hi-solid/PlusIcon {:class "-ml-1 -mr-0.5 h-5 w-5 text-gray-400"}]
     "Добавить домен"]]
   (when (#{"admin"} role)
     [:<>
      [:div.mt-10 {:class "bg-white shadow-sm ring-1 ring-gray-900/5"}
       [h-ui/disclosure
        {:as :section
         :class "grid items-center"}
        [:h2.sr-only "Filters"]
        [:div {:class "relative col-start-1 row-start-1 py-4"}
         [:div {:class "mx-auto flex max-w-7xl space-x-6 divide-x divide-gray-200 px-4 text-sm sm:px-6 lg:px-8"}
          [:div
           [h-ui/disclosure-button
            {:class "group flex items-center font-medium text-gray-700"}
            [:> hi-solid/FunnelIcon {:class "mr-2 h-5 w-5 flex-none text-gray-400 group-hover:text-gray-500"}]
            "Фильтры"]]
          [:div.pl-6
           [:button.text-gray-500
            "Очистить всё"]]]]
        [:div {:class "col-start-1 row-start-1 py-4"}
         [:div {:class "mx-auto flex max-w-7xl justify-end px-4 sm:px-6 lg:px-8"}
          [f-inputs/dropdown form/filter-path [:group-by]]]]
        [h-ui/disclosure-panel
         {:class "py-10 border-t"}
         [:div {:class "mx-auto grid max-w-7xl grid-cols-2 gap-x-4 px-4 text-sm sm:px-6 md:gap-x-6 lg:px-8"}
          [:div {:class "grid auto-rows-min grid-cols-1 gap-y-10 md:grid-cols-2 md:gap-x-6"}
           [:fieldset
            [:legeng "Пользователь"]
            [:div {:class "space-y-6 pt-6 sm:space-y-4 sm:pt-4"}
             [:div {:class "flex items-center text-base sm:text-sm"}
              [:input
               {:class "h-4 w-4 flex-shrink-0 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                :type "checkbox"}]]]]]]]]]
      [:div {:class "bg-gray-100 ring-1 ring-gray-900/5"}
       [:div {:class "mx-auto max-w-7xl px-4 py-3 sm:flex sm:items-center"}
        [:div {:class "mt-2 sm:mt-0"}
         [:div {:class "flex flex-wrap items-center"}
          [:span {:class "m-1 inline-flex items-center rounded-full border border-gray-200 bg-white py-1.5 pl-3 pr-2 text-sm font-medium text-gray-900"}
           [:span.text-xs [:span.text-gray-500 "Пользователь: "] [:span.font-medium "Долбаёб"]]
           [:button {:class "ml-1 inline-flex h-4 w-4 flex-shrink-0 rounded-full p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-500"}
            [:svg
             {:class "h-2 w-2",
              :stroke "currentColor",
              :fill "none",
              :viewBox "0 0 8 8"}
             [:path
              {:stroke-linecap "round", :strokeWidth "1.5", :d "M1 1l6 6m0-6L1 7"}]]]]]]]]])
   [:table.mt-10 {:class "w-full text-left bg-white"}
    [:thead.sr-only
     [:tr
      [:th "Домен"]
      [:th "Провайдер"]]]
    [:tbody
     {:class "divide-y divide-gray-100 shadow-sm ring-1 ring-gray-900/5"}
     (for [[k items] grouped-items]
       ^{:key k}
       [:<>
        [:tr {:class "text-sm leading-6 text-gray-900 bg-gray-50"}
         [:td {:class "relative isolate py-2 font-semibold px-4"}
          [:div k]]
         [:td]
         [:td]]
        (for [i items]
          ^{:key (str k "-" (:id i))}
          [:tr
           [:td {:class "relative py-5 px-4"}
            [:div
             [:div {:class "text-sm font-medium leading-6 text-gray-900"}
              (:domain i)]
             [:div {:class "block sm:hidden"}
              [:div {:class "text-xs leading-6 text-gray-500"}
               "Провайдер: " [:span.font-semibold (:provider i)]]
              [:div {:class "text-xs leading-5 text-gray-500"}
               "IP: " [:span.font-semibold (:ip i)]]]
             [:div
              [:div {:class "text-xs leading-6 text-gray-500"}
               "Пользователь: " [:span.font-semibold (or (:user i) "-")]]
              [:div {:class "text-xs leading-5 text-gray-500"}
               "Добавлен: " [:span.font-semibold (:created_at i)]]]]]
           [:td {:class "px-4 hidden py-5 pr-6 sm:table-cell"}
            [:div {:class "text-sm leading-6 text-gray-900"}
             "Провайдер: " [:span.font-semibold (:provider i)]]
            [:div {:class "text-xs leading-5 text-gray-500"}
             "IP: " [:span.font-semibold (:ip i)]]]
           [:td {:class "px-4 py-5 text-right relative"}
            [:div {:class "flex justify-end"}
             [:div.flex.absolute.top-5.right-4 (:status i)]
             (cond
               (#{"team_lead"} role)
               [:button
                {:class "text-sm font-medium leading-6 text-brazz-600 hover:text-brazz-500"
                 :on-click #(dispatch [::ctrl/request-replace-domain i])}
                "Заменить"
                [:span {:class "hidden sm:inline"} " домен"]]

               (#{"admin"} role)
               [:a
                {:href (:href i)
                 :class "text-sm font-medium leading-6 text-brazz-600 hover:text-brazz-500"}
                "Редактировать"
                [:span {:class "hidden sm:inline"} " домен"]])]]])])]]])

(model/reg-domains-page view)