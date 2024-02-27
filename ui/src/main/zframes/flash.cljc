(ns zframes.flash
  (:require [re-frame.core :as rf]
            [reagent.core :as r]

            [clojure.string :as str]
            [headlessui-reagent.core :as h-ui]
            
            ["@heroicons/react/24/solid" :as hi-solid]))

(defn gen-uuid
  []
  #?(:clj (java.util.UUID/randomUUID)
     :cljs (random-uuid)))

(rf/reg-event-db
 ::flash
 (fn [db [_ status data-or-params maybe-params]]
   (let [{:keys [id header body]
          :or   {id (keyword (str (gen-uuid)))}}
         (or maybe-params data-or-params)]
     #?(:cljs (do
                (assoc-in db [:flash id] {:st status :b body :h header}))
        :clj  (do
                (assoc-in db [:flash id] {:st status :b body :h header}))))))

(rf/reg-event-db ::remove-flash (fn [db [_ id]] (update db :flash dissoc id)))

(rf/reg-fx :flash/flash (fn [[status & args]] (rf/dispatch (apply vector ::flash status args))))

(doseq [type [:success :error :warning :info :notification]]
  (let [ev (keyword "flash" (name type))]
    (rf/reg-event-fx ev (fn [_ [_ & args]] {:flash/flash (vec (cons type args))}))))

(rf/reg-sub ::flashes (fn [db _] (:flash db)))

(defn flash-layout
  [*id {:keys [time] :or {time 8000}} _]
  (let [!show?  (r/atom false)
        timeout time]
    (r/create-class
     {:component-did-mount 
      (fn [] 
        (reset! !show? true)
        #?(:cljs
           (js/setTimeout
            (fn []
              (reset! !show? false)
              (js/setTimeout
               (rf/dispatch [::remove-flash *id])
               500))
            timeout)))

      :reagent-render 
      (fn [id {:keys [h b]} {:keys [icon wr-c h-c b-c c-c]}]
        (let [show? @!show?]
          [h-ui/transition
           {:show show?
            :as   :div
            :class (str/split wr-c #" ")
            :data-flash-id id
            :enter "transform ease-out duration-300 transition"
            :enter-from "translate-y-2 opacity-0 sm:translate-y-0 sm:translate-x-2"
            :enter-to "translate-y-0 opacity-100 sm:translate-x-0"
            :leave "transition ease-in duration-100"
            :leave-from "opacity-100"
            :leave-to "opacity-0"}
           [:div
            [:div.p-4
             [:div.flex.items-start
              (when icon [:div.flex-shrink-0 icon])
              [:div.ml-3.w-0.flex-1.pt-0.5
               (when h [:p {:class h-c} h])
               (when b [:p {:class b-c} b])]
              [:div.ml-4.flex.flex-shrink-0
               [:button
                {:on-click #(reset! !show? false)
                 :class    c-c}
                [:span.sr-only "Close"]
                [:svg.h-5.w-5
                 {:viewBox     "0 0 20 20"
                  :fill        "currentColor"
                  :aria-hidden "true"}
                 [:path
                  {:d "M6.28 5.22a.75.75 0 00-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 101.06 1.06L10 11.06l3.72 3.72a.75.75 0 101.06-1.06L11.06 10l3.72-3.72a.75.75 0 00-1.06-1.06L10 8.94 6.28 5.22z"}]]]]]]]]))})))

(defmulti flash-msg (fn [_ f] (:st f)))

(defmethod flash-msg :default
  [id f]
  [flash-layout id f
   {:wr-c "pointer-events-auto w-full max-w-sm overflow-hidden rounded-lg bg-white shadow-lg ring-1 ring-black ring-opacity-5"
    :h-c  "text-sm font-medium text-gray-900"
    :b-c  "mt-1 text-sm text-gray-500"
    :c-c  "inline-flex rounded-md bg-white text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brazz-500 focus:ring-offset-2"}])

(defmethod flash-msg :error
  [id f]
  [flash-layout id f
   {:wr-c "pointer-events-auto w-full max-w-sm overflow-hidden rounded-lg bg-red-50 shadow-lg ring-1 ring-black ring-opacity-5"
    :h-c  "text-sm font-medium text-red-800"
    :b-c  "mt-1 text-sm text-red-700"
    :c-c  "inline-flex rounded-md bg-red-50 text-red-500 hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-red-600 focus:ring-offset-2 focus:ring-offset-red-50"
    :icon [:> hi-solid/XCircleIcon {:class "h-5 w-5 text-red-400"}]}])

(defmethod flash-msg :success
  [id f]
  [flash-layout id f
   {:wr-c "pointer-events-auto w-full max-w-sm overflow-hidden rounded-lg bg-green-50 shadow-lg ring-1 ring-black ring-opacity-5"
    :h-c  "text-sm font-medium text-green-800"
    :b-c  "mt-1 text-sm text-green-700"
    :c-c  "inline-flex rounded-md bg-green-50 text-green-500 hover:bg-green-100 focus:outline-none focus:ring-2 focus:ring-green-600 focus:ring-offset-2 focus:ring-offset-green-50"
    :icon [:> hi-solid/CheckCircleIcon {:class "h-5 w-5 text-green-400"}]}])

(defmethod flash-msg :info
  [id f]
  [flash-layout id f
   {:wr-c "pointer-events-auto w-full max-w-sm overflow-hidden rounded-lg bg-blue-50 shadow-lg ring-1 ring-black ring-opacity-5"
    :h-c  "text-sm font-medium text-blue-800"
    :b-c  "mt-1 text-sm text-blue-700"
    :c-c  "inline-flex rounded-md bg-blue-50 text-blue-500 hover:bg-blue-100 focus:outline-none focus:ring-2 focus:ring-blue-600 focus:ring-offset-2 focus:ring-offset-blue-50"
    :icon [:> hi-solid/InformationCircleIcon {:class "h-5 w-5 text-blue-400"}]}])

(defn flashes
  []
  (let [flashes (rf/subscribe [::flashes])]
    (fn []
      [:div
       {:aria-live "assertive",
        :class
        "pointer-events-none fixed inset-0 flex items-end px-4 py-6 sm:items-start sm:p-6"}
       (into [:div.flex.w-full.flex-col.items-center.space-y-4.sm:items-end]
             (reduce-kv (fn [acc k f]
                          (conj acc (flash-msg k f)))
                        [] @flashes))])))
