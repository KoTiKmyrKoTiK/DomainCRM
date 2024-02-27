(ns common.utils.macro
  "Macro-utils"
  #?(:cljs (:require-macros [common.utils.macro])))

(declare persist-scope ps)

;; Inner
;; ----------------------

(defmacro persist-scope
  "Takes local scope vars and defines them in the global scope. Useful for RDD.
   
   If given no symbols defs all visible vars."
  ([]
   (let [syms (cond-> &env
                (contains? &env :locals)
                :locals

                :always
                keys)]
     `(persist-scope ~@syms)))
  ([& syms]
   `(do
      ~@(map (fn [sym]
               `(def ~sym ~sym))
             syms))))

(def ^{:doc "An alias for `persist-scope`."} ps @#'persist-scope)

;; Experimental
;; ----------------------

(defmacro copy-meta!
  "Works only in clj. Does nothing in cljs.
   
   With no `k & ks` copies default keys, which is: `[:doc arglists]`."
  {:arglists '([sym from]
               [sym from [k & ks]])}
  ([sym from]
   (let [default [:doc :arglists]]
     `(copy-meta! ~sym ~from ~default)))
  ([sym from keys]
   `(doseq [k# ~keys]
      (alter-meta! (var ~sym) assoc k# (k# (meta (var ~from)))))))

#_
(defmacro map-bindings
  "Creates a map consisting of `{binding-name binding-value}` key-val pairs."
  [& bindings]
  (let [ks# (map keyword bindings)]
    ks#))

(defmacro copy-def
  "Binds `name` to the same var as `symbol`.
   
   Copies metadata.
   
   WIP! Doesn't really work."
  [name symbol]
  `(def ~name ~symbol))

'ok!
