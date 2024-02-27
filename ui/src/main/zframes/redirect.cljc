(ns zframes.redirect
  (:require [re-frame.core :as rf]
            [zframes.window-location :as window-location]
            [zframes.routing]
            [goog.crypt.base64 :as b64]
            #?(:cljs [clojure.string :as str])))

#?(:cljs
   (do
     (defn window-open
       [url]
       (try (.-focus (.open js/window url "_blank"))
            (catch js/Error e (rf/dispatch [:flash/error {:msg "🠕  Разрешите всплывающие окна"}]))))

     (defn page-redirect
       [url]
       (set! (.-href (.-location js/window)) url))

     (rf/reg-fx
      ::page-redirect
      (fn [opts]
        (if (:_target opts)
          (let [window (window-open (:uri opts))]
            window)
          (page-redirect (str (:uri opts) (when-let [params (:params opts)]
                                            (->> params
                                                 (map (fn [[k v]] (str (name k) "=" (js/encodeURIComponent v))))
                                                 (str/join "&")
                                                 (str "?"))))))))

     (rf/reg-event-fx
      ::page-redirect
      (fn [_ [_ opts]]
        {::page-redirect opts}))

     (rf/reg-fx
      ::set-query-string
      (fn [params]
        (let [loc (.. js/window -location)]
          (.pushState
           js/history
           #js{} (:title params)
           (str (-> params
                    (dissoc :title)
                    (->> (reduce-kv
                          (fn [acc k v]
                            (assoc acc k
                                   (->> (b64/encodeString v))))
                          {}))
                    window-location/gen-query-string)
                (.-hash loc)))
          (zframes.routing/dispatch-context nil)
          (zframes.routing/dispatch-routes  nil))))))

(defn redirect
  [url]
  #?(:cljs (set!  (.-hash (.-location js/window)) url)
     :clj  (zframes.routing/rredirect url)))

(rf/reg-fx
 :window-open
 #?(:clj  (fn [& _]
            nil)
    :cljs (fn [url]
            (window-open url))))

(rf/reg-fx
 ::redirect
 (fn [opts]
   (redirect (str (:uri opts)
                  (when-let [params (:params opts)]
                    (window-location/gen-query-string params))))))

(rf/reg-event-fx
 ::redirect
 (fn [fx [_ opts]]
   {::redirect opts}))

(rf/reg-event-fx
 ::merge-params
 (fn [{db :db} [_ params]]
   (let [pth (get db :fragment-path)
         nil-keys (reduce (fn [acc [k v]]
                            (if (nil? v) (conj acc k) acc)) [] params)
         old-params (or (get-in db [:fragment-params :params]) {})]
     {::redirect {:uri pth
                  :params (apply dissoc (merge old-params params)
                                 nil-keys)}})))

(rf/reg-event-fx
 ::set-params
 (fn [{db :db} [_ params]]
   (let [pth (get db :fragment-path)]
     {::redirect {:uri pth
                  :params params}})))
