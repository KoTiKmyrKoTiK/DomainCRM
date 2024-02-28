(ns app.zenform-events
  (:require [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx
 ::make-init-display
 (fn [_ [_ {:keys []}]]))