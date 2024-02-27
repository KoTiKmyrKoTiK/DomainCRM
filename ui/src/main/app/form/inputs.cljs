(ns app.form.inputs
  (:require [clojure.string :as str]

            [goog.dom :as gdom]

            [reagent.core    :as r]
            [re-frame.core   :as rf]

            [zenform.model :as zf]

            [app.helpers   :as h]
            [app.form.mask :as m]

            [headlessui-reagent.core :as h-ui]
            ["@heroicons/react/20/solid" :as heroicons])
  (:import [goog.async Debouncer]))

(defn debounce
  [f interval]
  (let [dbnc (Debouncer. f interval)]
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

(defn target-value
  [e]
  (.. e -target -value))

(defn input-empty?
  [event]
  (let [caret-start-idx (.. event -target -selectionStart)
        caret-end-idx   (.. event -target -selectionEnd)]
    (and (= caret-start-idx 0) (= caret-end-idx 0))))

(defn focus-next-input
  [event]
  (some->> (.-target event)
           (iterate #(.-nextElementSibling %))
           (next)
           (filter (some-fn nil? #(= "INPUT" (.-tagName %))))
           (first)
           (.focus)))

(defn focus-prev-input
  [event]
  (some->> (.-target event)
           (iterate #(some-> % gdom/getPreviousElementSibling))
           (next)
           (filter (some-fn nil? #(= "INPUT" (.-tagName %))))
           (first)
           (.focus)))

(defn focus-next-element
  [event selector]
  (let [selector-property (case (first selector)
                            "." #(.-className %)
                            #(.-id %))
        selector'         (str/replace selector #"\.|#" "")]
    (some->> (.-target event)
             (iterate gdom/getNextNode)
             (filter (some-fn nil? #(= selector' (selector-property %))))
             (first)
             (.focus))))

(defn focus-prev-element
  [event selector]
  (let [selector-property (case (first selector)
                            "." #(.-className %)
                            #(.-id %))
        selector'         (str/replace selector #"\.|#" "")]
    (some->> (.-target event)
             (iterate gdom/getPreviousNode)
             (filter (some-fn nil? #(= selector' (selector-property %))))
             (first)
             (.focus))))

(defn select-value
  [event]
  (.. event -target (setSelectionRange 0 (.. event -target -value -length))))

(defn invalid-feedback
  [form-path path & [_]]
  (let [node (rf/subscribe [:zf/node form-path path])]
    (fn [& _]
      (let [errs (some->> (:errors @node)
                          (filter (fn [[k v]]
                                    (keyword? k)))
                          (into {})
                          (merge (get (:errors @node) (vector))))]
        (when errs [:div.text-error (str/join ", " (vals errs))])))))

(defn label
  {:arglists '([form-path path & {:keys [label required]}])}
  [form-path path & _]
  (let [node (rf/subscribe [:zf/node form-path path])]
    (fn label
      [_ _ & {:keys [label] :as args}]
      (let [{:keys [validators]} @node
            label'  (or label (:label @node))]
        (when label'
          [:label.text-label
           {:class [(when (or (seq (select-keys validators [:required :pseudo]))
                              (:required args))
                      :required)]}
           label'])))))

(defn text
  [form-path path & _]
  (let [node          (rf/subscribe [:zf/node form-path path])
        default-class :border-base
        class         (r/atom default-class)]
    (fn [_ path & [{:keys [mask placeholder on-change no-margin? no-label? external-errors disabled-with-border
                           errors-on-disable? addition preaddition no-error-text?] :as args}]]
      (let [{:keys [value masked-value validators errors] field-type :type :as form-node} @node
            disabled        (or (:disabled args) (:disabled form-node))
            label'          (or (:label args) (:label @node))
            errors*         (merge errors external-errors)
            value*          (cond
                              (int? value) value
                              (number? value) value
                              (not-empty value) value
                              :else nil)
            show-clear-btn? (and value* (not disabled))]
        [:div
         (when (and label' (not no-label?)) [:label.block.text-sm.font-medium.leading-6.text-gray-900 (when (or (seq (select-keys validators [:required :pseudo])) (:required args)) {:class [:required]}) label'])
         [:div (when (and label' (not no-label?))
                 {:class "mt-2"})
          [:input.block.w-full.rounded-md.border-0.py-1.5.text-gray-900.shadow-sm.ring-1.ring-inset.ring-gray-300.placeholder:text-gray-400.focus:ring-2.focus:ring-inset.focus:ring-brazz-600.sm:text-sm.sm:leading-6
           (merge {:type        (or (:input-type args) :text)
                   :value       (if (:unmask? mask)
                                  (or masked-value (m/mask-resolve (select-keys mask [:mask]) value*))
                                  value*)
                   :disabled    (or disabled disabled-with-border)
                   :placeholder placeholder
                   :on-focus    #(reset! class :border-focus)
                   :on-blur     #(reset! class :border-base)
                   :on-change   #(if disabled
                                   (.preventDefault %)
                                   (let [node-value      (.. % -target -value)
                                         processed-value (cond
                                                           (:unmask? mask)
                                                           (m/unmask mask node-value)

                                                           mask
                                                           (m/mask-resolve mask node-value)

                                                           :else
                                                           node-value)]
                                     (rf/dispatch [:zf/set-value form-path path
                                                   (cond
                                                     (#{:integer} field-type)
                                                     (when-not (.isNaN js/Number (h/parseInt processed-value))
                                                       (h/parseInt processed-value))

                                                     (#{:positive-integer} field-type)
                                                     (let [num (h/parseInt processed-value)]
                                                       (when-not (or (.isNaN js/Number num)
                                                                     (< num 0))
                                                         num))

                                                     (#{:float} field-type)
                                                     (let [num-str (m/mask-resolve {:mask #"^\d+\.?\d*$"} processed-value)]
                                                       (when-not (.isNaN js/Number (h/parseFloat num-str))
                                                         num-str))

                                                     :else
                                                     processed-value)])
                                     (when (:unmask? mask)
                                       (rf/dispatch [:zf/update-node-schema form-path path
                                                     {:masked-value (if mask
                                                                      (m/mask-resolve (select-keys mask [:mask])
                                                                                      node-value)
                                                                      node-value)}]))
                                     (when on-change (on-change node-value))))}
                  (when (and (not no-error-text?)
                             (seq errors*)
                             (or (not disabled) errors-on-disable?))
                    {:class "text-red-900 ring-1 ring-inset ring-red-300 placeholder:text-red-300 focus:ring-2 focus:ring-inset focus:ring-red-500 sm:text-sm sm:leading-6"})
                  (:props args))]
          (when (and (not no-error-text?)
                     (seq errors*)
                     (or (not disabled) errors-on-disable?))
            [:p.mt-2.text-sm.text-red-600 (str/join ", " (vals errors*))])]]))))

(defn on-search-event
  [on-search search-params]
  (let [[event event-params] (if (keyword? on-search) (vector on-search) on-search)]
    [event (-> (merge-with merge event-params search-params)
               (#(update % :params merge (:search-params %))))]))

(defn dropdown
  [form-path path & [args]]
  (let [!node     (rf/subscribe [:zf/node form-path path])]
    (fn [_ path & [{:keys [label on-select no-label? no-margin? disabled display-fn on-clear]}]]
      (let [{:keys [value validators errors items] :as form-node} @!node
            disabled* (or disabled (:disabled form-node))
            display   (or (when display-fn (display-fn value))
                          (some #(when (#{value} (:value %)) (:display %)) items)
                          disabled*)
            label'    (or label (:label form-node))

            item-by-value (zipmap (map :value items) items)]
        (when-not value (rf/dispatch [:zf/set-value form-path path (-> items first :value)]))
        [:div
         [h-ui/listbox
          {:value     value
           :on-change #(do (rf/dispatch [:zf/set-value form-path path %])
                           (when on-select (on-select % (get item-by-value %))))}
          (when (and label' (not no-label?))
            [h-ui/listbox-label {:class "block text-sm font-medium leading-6 text-gray-900"}
             label'])
          [:div {:class "relative mt-2"}
           [h-ui/listbox-button
            {:class "relative w-full cursor-default rounded-md bg-white py-1.5 pl-3 pr-10 
                                  text-left text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 
                                  focus:outline-none focus:ring-2 focus:ring-brazz-600 sm:text-sm sm:leading-6"}
            [:span {:class "block truncate"} display]
            [:span {:class "pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2"}
             [:> heroicons/ChevronUpDownIcon {:class "h-5 w-5 text-gray-400"}]]]

           [h-ui/transition
            {:leave "transition ease-in duration-100"
             :leave-from "opacity-100"
             :leave-to "opacity-0"}
            [h-ui/listbox-options
             {:class "absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white 
                                   py-1 text-base shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm"}
             (for [i items]
               ^{:key (:value i)}
               [h-ui/listbox-option
                {:value (:value i)
                 :class (fn [{:keys [active]}]
                          (concat ["relative" "cursor-default" "select-none" "py-2" "pl-8" "pr-4"]
                                  (if active
                                    ["bg-brazz-600" "text-white"]
                                    ["text-gray-900"])))}
                (fn [{:keys [selected active]}]
                  [:<>
                   [:span.block.truncate {:class (if selected :font-medium :font-normal)}
                    (:display i)]
                   (when selected
                     [:span
                      (cond-> {:class (str/split "absolute inset-y-0 left-0 flex items-center pl-1.5" #" ")}
                        active       (update :class concat ["text-white"])
                        (not active) (update :class concat ["text-brazz-600"]))
                      [:> heroicons/CheckIcon 
                       {:class "w-5 h-5" :aria-hidden "true"}]])
                   (when selected
                     [:span.absolute.inset-y-0.left-0.flex.items-center.pl-3.text-amber-600
                      ])])])]]]]
         (when (and (not disabled*) (or (seq errors) (:error args)))
           [:p.mt-2.text-sm.text-red-600 (str/join ", " (vals errors))])]))))
