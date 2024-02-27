(ns app.re-frame-helpers
  (:require [re-frame.core :refer [reg-sub reg-event-db]]))

(reg-sub
 :db/get
 (fn [db [_ path]]
   (get-in db (if (vector? path) path [path]))))

(reg-event-db
 :db/update
 (fn [db [_ path func]]
   (if (vector? path)
     (update-in db path func)
     (update db path func))))

(reg-event-db
 :db/assoc
 (fn [db [_ path value]]
   (if (vector? path)
     (assoc-in db path value)
     (assoc db path value))))
