(ns app.domain-server-providers.view
  (:require [re-frame.core :refer [dispatch]]

            [zenform.core :as zf]

            [app.pages       :as pages]
            [app.form.inputs :as inputs]

            [app.components :as cmp]

            [app.domain-server-providers.model      :as model]
            [app.domain-server-providers.controller :as ctrl]

            [app.domain-server-providers.crud.view]
            
            ["@heroicons/react/24/solid" :as hi-solid]))

(defn view
  [data _]
  [:<>
   [cmp/title-divider "Провайдеры серверов доменов"
    [:a {:href (:create-href data)
         :class "inline-flex items-center gap-x-1.5 rounded-full bg-white px-3 py-1.5 text-sm 
                 font-semibold text-gray-900 shadow-sm 
                 ring-1 ring-inset ring-gray-300 hover:bg-gray-50"}
     [:> hi-solid/PlusIcon {:class "-ml-1 -mr-0.5 h-5 w-5 text-gray-400"}]
     "Добавить провайдера серверов"]]
   [:ul.mt-10 {:role "list" :class "divide-y divide-gray-100 overflow-hidden bg-white shadow-sm ring-1 ring-gray-900/5 sm:rounded-xl"}
    (for [dsp (:items data)]
      ^{:key (:id dsp)}
      [:li
       [:a {:href (:href dsp)
            :class "relative flex justify-between items-center gap-x-6 px-4 py-5 hover:bg-gray-50 sm:px-6 lg:px-8"}
        [:div {:class "flex min-w-0 gap-x-4"}
         [:div {:class "min-w-0 flex-auto"}
          [:div {:class "flex items-start gap-x-3"}
           [:p {:class "text-sm font-semibold leading-6 text-gray-900"}
            (:name dsp)]]]]
        [:> hi-solid/ChevronRightIcon {:class "h-5 w-5 flex-none text-gray-400"}]]])]])

(model/reg-domain-server-providers-page view)