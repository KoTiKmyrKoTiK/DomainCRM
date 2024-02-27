(ns app.login.model
  (:require [clojure.string :as str]

            [re-frame.core :refer [reg-sub]]

            [app.helpers :as h]
            [app.pages :as pages]
            
            [common.routes.login :as pid]))

(def reg-login-page (pages/reg-page-fn pid/login))

(reg-sub
 pid/login
 (fn [db] {}))
