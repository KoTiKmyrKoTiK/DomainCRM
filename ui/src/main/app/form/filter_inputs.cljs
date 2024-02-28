(ns app.form.filter-inputs
  (:require [clojure.string :as str]

            [goog.dom :as gdom]

            [reagent.core    :as r]
            [re-frame.core   :as rf]

            [app.form.inputs :as fi]

            [headlessui-reagent.core :as h-ui]
            ["@heroicons/react/24/solid" :as hi-solid])
  (:import [goog.async Debouncer]))

(defn dropdown
  [form-path path & [args]]
  (let [!node     (rf/subscribe [:zf/node form-path path])]
    (fn [_ path & [{:keys [label on-select no-label? no-margin? disabled display-fn on-clear]}]]
      (let [{:keys [value validators errors items] :as form-node} @!node
            disabled* (or disabled (:disabled form-node))
            display   (or (when display-fn (display-fn value))
                          (->> items
                               (filter (comp (partial = value) :value))
                               first
                               :display)
                          disabled*)
            label'    (or label (:label form-node))

            item-by-value (zipmap (map :value items) items)

            f-items (remove (comp (partial = value) :value) items)]
        [:div
         [h-ui/menu
          {:as :div
           :class "relative inline-block"}
          [:div.flex
           [h-ui/menu-button
            {:class "group inline-flex justify-center text-sm font-medium text-gray-700 hover:text-gray-900"}
            [:span
             [:span
              (when value {:class "text-gray-500"})
              label'] 
             (when value [:span.font-medium ": " display])]
            [:> hi-solid/ChevronDownIcon {:class "-mr-1 ml-1 h-5 w-5 flex-shrink-0 text-gray-400 group-hover:text-gray-500"}]]]
          [h-ui/transition
           {:as         :div
            :class      "absolute right-0 z-10 mt-2 w-40 origin-top-right rounded-md bg-white shadow-2xl ring-1 ring-black ring-opacity-5 focus:outline-none"
            :enter      "transition ease-out duration-100"
            :enter-from "transform opacity-0 scale-95"
            :enter-to   "transform opacity-100 scale-100"
            :leave      "transition ease-in duration-75"
            :leave-from "transform opacity-100 scale-100"
            :leave-to   "transform opacity-0 scale-95"}
           [h-ui/menu-items
            [:div {:class "py-1"}
             (for [i f-items]
               [h-ui/menu-item
                {:key (str "menu-" (:value i))}
                (fn [{:keys [active]}]
                  [:button
                   (cond-> {:class ["text-left" "w-full" "block" "px-4" "py-2" "text-sm"]
                            :on-click #(do (rf/dispatch [:zf/set-value form-path path (:value i)])
                                           (when on-select (on-select (:value i) (get item-by-value (:value i)))))}
                     (= value (:value i))
                     (update :class concat ["font-medium" "text-gray-900"])

                     (nil? (:value i))
                     (update :class concat ["text-gray-500"])

                     active
                     (update :class concat ["bg-gray-100"]))
                   (:display i)])])]]]]]))))