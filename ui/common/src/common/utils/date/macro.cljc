(ns common.utils.date.macro
  #?(:cljs (:require-macros [common.utils.date.macro])))

(defmacro def-format
  [name doc-string format]
  `(defn ~(with-meta name (assoc (meta name)
                                 :common.utils.date/format true))
     ~doc-string
     ([]
      ~format)
     ([~'s]
      (common.utils.date/format-date ~'s (~name)))))
