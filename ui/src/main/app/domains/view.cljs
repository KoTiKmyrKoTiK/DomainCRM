(ns app.domains.view
  (:require [re-frame.core :refer [dispatch]]

            [zenform.core :as zf]

            [app.pages       :as pages]
            [app.form.inputs :as inputs]

            [app.components :as cmp]

            [app.domains.model      :as model]
            [app.domains.controller :as ctrl]

            [app.domains.crud.view]
            
            ["@heroicons/react/24/solid" :as hi-solid]))

(defn view
  [data _]
  [:<>
   [cmp/title-divider "Домены"
    [:a {:href (:create-href data)
         :class "inline-flex items-center gap-x-1.5 rounded-full bg-white px-3 py-1.5 text-sm 
                 font-semibold text-gray-900 shadow-sm 
                 ring-1 ring-inset ring-gray-300 hover:bg-gray-50"}
     [:> hi-solid/PlusIcon {:class "-ml-1 -mr-0.5 h-5 w-5 text-gray-400"}]
     "Добавить домен"]]
   [:table.mt-10 {:class "w-full text-left bg-white"}
    [:thead.sr-only
     [:th "Домен"]
     [:th "Провайдер"]]
    [:tbody
     {:class "divide-y divide-gray-100 shadow-sm ring-1 ring-gray-900/5 sm:rounded-xl"}
     (for [[k items] (:items data)]
       ^{:key k}
       [:<>
        [:tr {:class "text-sm leading-6 text-gray-900"}
         [:th {:class "relative isolate py-2 font-semibold px-4"}
          [:div k]]]
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
             [:a
              {:href (:href i)
               :class "text-sm font-medium leading-6 text-brazz-600 hover:text-brazz-500"}
              "Редактировать"
              [:span {:class "hidden sm:inline"} " домен"]]]]])])]]])

(model/reg-domains-page view)