(ns app.domain-server-providers.crud.view
  (:require [re-frame.core :refer [dispatch]]

            [app.form.inputs :as inputs]

            [app.components :as cmp]

            [app.domain-server-providers.crud.model      :as model]
            [app.domain-server-providers.crud.form       :as form]
            [app.domain-server-providers.crud.controller :as ctrl]))

(defn view
  [data _]
  [:<>
   [:div.hidden.lg:block
    [cmp/title-divider (:title data)]]
   [:form.lg:mt-10 {:class "bg-white shadow-sm ring-1 ring-gray-900/5 sm:rounded-xl md:col-span-2"}
    [:div {:class "px-4 py-6 sm:p-8"}
     [:div {:class "grid max-w-2xl grid-cols-1 gap-x-6 gap-y-8 sm:grid-cols-6"}
      [:div.col-span-full
       [inputs/text form/form-path [:name]]]]]
    [:div {:class "flex items-center justify-end gap-x-6 border-t border-gray-900/10 px-4 py-4 sm:px-8"}
     [:button
      {:class "text-sm font-semibold leading-6 text-gray-900"
       :on-click #(dispatch [:zframes.redirect/redirect {:uri "#/domain_server_providers"}])}
      "Отмена"]
     [:button
      {:class "rounded-md bg-brazz-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-brazz-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brazz-600"
       :on-click #(dispatch [::ctrl/save-flow])}
      "Сохранить"]]]])

(model/reg-create-page view)
(model/reg-edit-page view)
