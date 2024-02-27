(ns app.home.view
  (:require [app.pages       :as pages]
            [app.form.inputs :as inputs]

            [app.home.model      :as model]
            [app.home.controller :as ctrl]))

(defn view
  [_ _])

(model/reg-dashboard-page view)