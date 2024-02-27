(ns app.pages
  (:require [re-frame.core :as rf]))

(defonce pages (atom {}))

(rf/reg-sub
 :pages/get-in
 (fn [db [_ path]]
   (get-in db path)))

(rf/reg-sub
 :pages/data
 (fn [db [_ pid]]
   (get db pid)))

(rf/reg-sub
 :pages/pth
 (fn [db [_ pid pth]]
   (-> db
       (get pid)
       (get-in pth))))

(rf/reg-event-db
 :pages/merge
 (fn [db [_ pid data]]
   (update db pid merge data)))

(rf/reg-event-db
 :pages/dissoc
 (fn [db [_ pid key]]
   (update db pid dissoc key)))

(rf/reg-event-db
 :pages/assoc-in
 (fn [db [_ pid path data]]
   (update db pid assoc-in path data)))

(defn reg-page
  [key page]
  (swap! pages assoc key page))

(defn subscribed-page
  "Returns second form react component that renders view
   with derefed model as first argument
   and given params as second."
  [page-idx view]
  (fn subscribed-page
    [_]
    (let [model (rf/subscribe [page-idx])]
      (fn subscribed-page
        [params]
        [view @model params]))))

(defn reg-subs-page
  "register subscribed page under keyword for routing."
  [idx view & [layout-key]]
  (swap! pages assoc idx (subscribed-page idx view)))

(rf/reg-sub
 :config
 (fn [db path]
   (get-in db path)))

(defn reg-page-fn
  [pid]
  (comp
   (partial reg-page pid)
   (partial subscribed-page pid)))
