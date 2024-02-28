(ns app.domains.crud.view
  (:require [re-frame.core :refer [dispatch]]

            [app.form.inputs :as inputs]

            [app.components :as cmp]

            [app.domains.crud.model      :as model]
            [app.domains.crud.form       :as form]
            [app.domains.crud.controller :as ctrl]))

(defn view
  [data _]
  [:div.mx-auto.max-w-3xl
   [:div.hidden.lg:block
    [cmp/title-divider (:title data)]]
   [:form.lg:mt-10 {:class "bg-white shadow-sm ring-1 ring-gray-900/5 sm:rounded-xl md:col-span-2"}
    [:div {:class "px-4 py-6 sm:p-8"}
     [:div {:class "grid max-w-2xl grid-cols-1 gap-x-6 gap-y-8 sm:grid-cols-6"}
      [:div.sm:col-span-3
       [inputs/text form/form-path [:subdomain]]]
      [:div.sm:col-span-3
       [inputs/text form/form-path [:domain]]]
      [:div.col-span-full
       [inputs/text form/form-path [:server_ip]]]
      [:div.sm:col-span-3
       [inputs/dropdown form/form-path [:status]]]
      [:div.sm:col-span-3
       [inputs/dropdown form/form-path [:user_id]]]
      [:div.sm:col-span-3
       [inputs/dropdown form/form-path [:domain_server_provider_id]]]
      [:div.sm:col-span-3
       [inputs/dropdown form/form-path [:domain_provider_id]]]]]
    [:div {:class "flex items-center justify-end gap-x-6 border-t border-gray-900/10 px-4 py-4 sm:px-8"}
     [:button
      {:class "text-sm font-semibold leading-6 text-gray-900"
       :on-click #(dispatch [:zframes.redirect/redirect {:uri "#/domains"}])}
      "Отмена"]
     [:button
      {:class "rounded-md bg-brazz-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-brazz-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brazz-600"
       :on-click #(dispatch [::ctrl/save-flow])}
      "Сохранить"]]]])

(model/reg-create-page view)
(model/reg-edit-page view)
