(ns app.login.view
  (:require [re-frame.core :refer [dispatch]]

            [zenform.core :as zf]

            [app.pages       :as pages]
            [app.form.inputs :as inputs]

            [app.login.form       :as form]
            [app.login.model      :as model]
            [app.login.controller :as ctrl]))

(defn view
  [data _]
  [:div.flex.min-h-full.flex-col.justify-center.px-6.py-12.lg:px-8
   [:div.sm:mx-auto.sm:w-full.sm:max-w-sm
    [:img.mx-auto.h-10.w-auto {:src "/img/logo.png"}]
    [:h2.mt-10.text-center.text-2xl.font-bold.leading-9.tracking-tight.text-gray-900 "Войдите в аккаунт"]]
   [:div.mt-10.sm:mx-auto.sm:w-full
    {:class "sm:max-w-[480px]"}
    [:form.bg-white.px-6.py-12.shadow.sm:rounded-lg.sm:px-12
     [:div.space-y-6
      [inputs/text form/form-path [:email] {:input-type "email"}]
      [inputs/text form/form-path [:password] {:input-type "password"}]
      [:div
       [:button.flex.w-full.justify-center.rounded-md.bg-brazz-600.px-3.py-1.5.text-sm.font-semibold.leading-6.text-white.shadow-sm.hover:bg-brazz-500.focus-visible:outline.focus-visible:outline-2.focus-visible:outline-offset-2.focus-visible:outline-brazz-600
        {:on-click #(dispatch [::ctrl/submit])
         :type "submit"}
        "Войти"]]]]]])

(model/reg-login-page view)