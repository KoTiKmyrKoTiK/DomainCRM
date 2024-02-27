(ns zframes.storage
  (:require [re-frame.core :as rf]
            [goog.crypt.base64 :as b64]))

#?(:clj
   (defonce local-storage (atom {})))

#?(:cljs
   (defn keywordize
     [x]
     (js->clj x :keywordize-keys true)))

(defn- remove-item!
  [key]
  #?(:cljs (.removeItem (.-localStorage js/window) key)
     :clj  (swap! local-storage dissoc key)))

(defn- set-item!
  [key val]
  #?(:cljs (->> val
                clj->js
                (.stringify js/JSON)
                js/encodeURIComponent
                b64/encodeString
                (.setItem (.-localStorage js/window) (name key)))
     :clj  (swap! local-storage assoc key val)))

(defn get-item
  [key]
  #?(:cljs (try (->> key
                     name
                     (.getItem (.-localStorage js/window))
                     b64/decodeString
                     js/decodeURIComponent
                     (.parse js/JSON)
                     (keywordize))
                (catch js/Object _ (do (remove-item! key) nil)))
     :clj  (get @local-storage key)))

(rf/reg-cofx
 :storage/get
 (fn [coeffects keys]
   (reduce (fn [coef k]
             (assoc-in coef [:storage k] (get-item k)))
           coeffects keys)))

(rf/reg-fx
 :storage/set
 (fn [items]
   (doseq [[k v] items]
     (set-item! k v))))

(rf/reg-event-fx
 :storage/set
 (fn [_ [_ items]]
   {:storage/set items}))

(rf/reg-fx
 :storage/remove
 (fn [keys]
   (doseq [k keys]
     (remove-item! (name k)))))
