(ns app.components)

(defn title-divider
  ([t]
   (title-divider t nil))
  ([t button]
   [:div.relative
    [:div {:class "absolute inset-0 flex items-center"}
     [:div {:class "w-full border-t border-gray-300"}]]
    [:div {:class "relative flex justify-center lg:justify-between lg:items-center"}
     [:span {:class "hidden lg:block bg-gray-50 pr-3 text-base font-semibold leading-6 text-gray-900"}
      t]
     (if button button [:div.hidden])]]))