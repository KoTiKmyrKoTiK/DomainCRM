(ns app.form.filter-inputs
  (:require [clojure.string :as str]

            [goog.dom :as gdom]

            [reagent.core    :as r]
            [re-frame.core   :as rf]

            
            [zenform.model :as zf]

            [app.form.inputs  :as fi]
            [app.form.helpers :as fh]
            [app.helpers      :as h]

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

(defn chips-view
  [form-path & [{:keys [remove-chips-ev]}]]
  (letfn [(chips-data
            [filters]
            (->> filters
                 (filter (fn [[k v]] (and (not (#{:search} k)) (zf/get-value v))))
                 (map
                  (fn [[k v]]
                    (let [value   (:value v)
                          display (case (or (:filter-type v) (:type v))
                                    :object
                                    (-> v :value :display)

                                    :string
                                    (if (:items v)
                                      (->> (:items v)
                                           (h/find-first #(= value (:value %)))
                                           :display)
                                      value)

                                    :boolean
                                    (if v "Да" "Нет")

                                    :vector
                                    (if (string? value)
                                      (some-> value
                                              (str/split ",")
                                              (->> (map (fn [i]
                                                          (->> v :items
                                                               (h/find-first #(= i (get-in % [:value :id])))
                                                               :value :display)))
                                                   (str/join ", ")))
                                      (->> value (map :display) (str/join ", ")))

                                    value)]
                      {:path     k
                       :title    (:label v)
                       :disabled (-> v :status :disabled)
                       :remove   #(rf/dispatch [::fh/clear-filter-value form-path k v {:remove-chips-ev remove-chips-ev}])
                       :display  display})))
                 (remove #(not (string? (:display %))))))]
    (let [filter-node (rf/subscribe [:zf/node form-path])
          filters     (r/track #(:value @filter-node))]
      (fn [_ & [{:keys [params]}]]
        (let [filters (chips-data @filters)]
          [:div {:class "flex flex-wrap items-center"}
           [:<>
            (if (seq filters)
              (for [f filters]
                ^{:key (:path f)}
                [:span {:class "m-1 inline-flex items-center rounded-full border border-gray-200 bg-white py-1.5 pl-3 pr-2 text-sm font-medium text-gray-900"}
                 [:span.text-xs
                  [:span.text-gray-500 (str (:title f) ": ")]
                  [:span.font-medium (:display f)]]
                 (when-not (:disabled f)
                   [:button
                    {:title    "Убрать"
                     :class    "ml-1 inline-flex h-4 w-4 flex-shrink-0 rounded-full p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-500"
                     :on-click (:remove f)}
                    [:svg
                     {:class   "h-2 w-2",
                      :stroke  "currentColor",
                      :fill    "none",
                      :viewBox "0 0 8 8"}
                     [:path
                      {:stroke-linecap "round"
                       :strokeWidth    "1.5"
                       :d              "M1 1l6 6m0-6L1 7"}]]])])
              [:span.text-gray-500.text-sm "Нет фильтров"])]])))))