(ns zframes.effects
  (:require [re-frame.core :refer [dispatch reg-fx]]
            #?@(:cljs [[goog.dom :as gdom]])))

(reg-fx
 :timeout
 #?(:clj  (fn [{:keys [event]}]
            (dispatch event))
    :cljs (fn [{:keys [event time]}]
            (js/setTimeout
             (fn []
               (dispatch event))
             time))))

(reg-fx
 :scroll-to-end
 #?(:clj  (fn [& _])
    :cljs (fn [{:keys [input-tag input-id]}]
            (let [input (->> (gdom/getElement input-id)
                             (gdom/getElementsByTagName input-tag)
                             (array-seq)
                             (first))]
              (aset input "scrollTop" (. input -scrollHeight))))))

