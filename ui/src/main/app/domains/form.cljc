(ns app.domains.form
  (:require  [re-frame.core :refer [reg-event-fx]]

             [zframes.mapper :as zm]

             [app.form.helpers :as fh]

             [common.utils.date :as cud]
             [app.helpers :as h]))

(def filter-path [:form :filter])

(def group-items
  [{:display "-Нет-"
    :value   nil}
   {:display "По провайдеру сервера"
    :label   "Сервер"
    :value   "server_provider"}
   {:display "По провайдеру домена"
    :label   "Провайдер"
    :value   "provider"}
   {:display "По IP"
    :label   "IP"
    :value   "ip"}
   {:display "По пользователю"
    :label   "Пользователь"
    :value   "user"}])

(def filter-schema
  {:type      :form
   :on-change [[::fh/validate-filter-form {:data {:form-path filter-path}}]]
   :fields    {:group-by               {:type  :string
                                        :label "Группировка"
                                        :items group-items}
               :user                   {:type        :string
                                        :label       "Пользователь"}
               :domain-provider        {:type        :string
                                        :label       "Провайдер домена"}
               :domain-server-provider {:type        :string
                                        :label       "Провайдер сервера"}}})

(defn form-init [_ [_ init]]
  {:fx (cond-> [[:dispatch [:zf/init filter-path filter-schema init]]]
         (seq init)
         (conj [:dispatch [:db/update filter-path #(assoc % :success true)]]))})