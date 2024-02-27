(ns app.login.model
  (:require [clojure.string :as str]

            [re-frame.core :refer [reg-sub]]

            [app.helpers :as h]
            [app.pages :as pages]
            
            [common.routes :as cr]))

(def reg-login-page (pages/reg-page-fn cr/pid-login))

(reg-sub
 cr/pid-login
 (fn [db] {}))
