(ns common.utils
  (:require [clojure.set          :as set]
            [clojure.string       :as str]

            [com.rpl.specter :as sp]

            [common.utils.core    :as core]
            [common.utils.request :as request]
            [common.utils.macro   :as macro]
            [common.utils.date    :as dh]
            #?(:clj  clojure.pprint
               :cljs cljs.pprint)
            #?@(:cljs [[goog.string]]))
  #?(:clj (:refer-clojure :exclude [format])))

(macro/copy-def parse-int core/parse-int)
(macro/copy-def parse-num core/parse-num)
(macro/copy-def rpartial  core/rpartial)

(defn map->query-string
  ^String [params]
  (->> params
       (map (fn [[k v]] (str (name k) \= v)))
       (str/join "&")))

(defn to-url-query
  ^String [url params]
  (str url \? (map->query-string params)))

(macro/copy-def format     core/format)
(macro/copy-def format-str core/format)

(defn lowercase-first-letter [s]
  (str/replace-first s (subs s 0 1) (str/lower-case (subs s 0 1))))

(defn- trim
  "Remove `n` number of characters from both ends of string."
  #?(:clj  ^String [^Integer n ^String s]
     :cljs ^String [^number  n ^String s])
  (if-not (< (count s) (* 2 n))
    (subs s n (- (count s) n))
    ""))

(defn format-obj
  "Reduces object to string using given format."
  ^String [^String fmt m & [^String na-placeholder]]
  (let [na-placeholder' (or na-placeholder "")
        re-subs         #"%[^ ].+?%"
        ks              (->> (re-seq re-subs fmt)
                             (map (comp keyword (partial trim 1))))
        fns             (map (fn [k]
                               #(k % na-placeholder')) ks)
        fmt-            (str/replace fmt re-subs "%s")]
    (apply (partial format fmt-)
           ((apply juxt fns) m))))

(defn pprint
  [x]
  (#?(:clj  clojure.pprint/pprint
      :cljs cljs.pprint/pprint)
   x))

(defn p
  "Pretty prints arg and returns it.
   
  Useful for debug, especially in ->> and -> macros."
  [arg & meta]
  (when meta (apply println meta))
  (pprint arg)
  arg)

(defn identity-default
  [default value]
  (if (str/blank? value) default value))

(defn upper-first-letter
  [value]
  (str (str/upper-case (first value))
       (subs value 1)))

(defn safe-capitalize
  [value]
  (some-> value
          (str/replace #"[^\.|\s|-]+" #(str/capitalize %1))))

(defn fio
  "Reduces name object to a string."
  ^String [name]
  (->> name
       (into {})
       ((juxt (comp safe-capitalize :family)
              (comp (partial str/join " ")
                    (partial map safe-capitalize)
                    :given)))
       (str/join " ")
       ((partial identity-default "Нет данных"))))

(defn fio-initials
  [name]
  (if (vector? name)
    (some->> name first
             ((juxt (comp safe-capitalize :family)
                    (comp str/join
                          (partial map (comp #(str % ".") str/capitalize first))
                          :given)))
             (str/join " ")
             ((partial identity-default "Нет данных")))
    (some-> name
            (str/split #"\s")
            ((juxt first
                   (comp str/join
                         (partial map (comp #(str % ".") first))
                         rest)))
            (->> (str/join " "))
            ((partial identity-default "Нет данных")))))

(defn recursive-replace
  [s replacements]
  (reduce (fn [acc {pattern :pattern replacement :replacement}]
            (str/replace acc pattern replacement))
          s replacements))

(def ^{:arglists '([pred coll])} matches?
  "Tests that every item in `pred` either equal to the corresponding value in `coll` or
   returns logical true when run against it.
   
   Useful for `condp`."
  (comp (partial every? true?) (partial map (fn [test val]
                                              (or (= test val)
                                                  (try
                                                    (boolean (test val))
                                                    (catch #?(:clj  Exception
                                                              :cljs js/Error) _
                                                      false)))))))

(defn deep-merge
  "Efficient deep merge."
  [& maps]
  (letfn [(deep-merge*
            [a b]
            (loop [[[k bv :as i] & ks] b
                   acc a]
              (if (nil? i)
                acc
                (let [av (get a k)]
                  (if (= bv av)
                    (recur ks acc)
                    (recur ks (if (and (map? bv) (map? av))
                                (assoc acc k (deep-merge av bv))
                                (assoc acc k bv))))))))]
    (reduce deep-merge* maps)))

;; Strips
;; ---------------------------------------
(defn dissoc-when
  "Dissocs `k` from `m` when `pred` called with its value returns true.
   
   Works with indices for vectors."
  ([pred m k]
   (cond-> m
     (and (contains? m k)
          (pred (get m k)))
     (dissoc k)))
  ([pred m k & ks]
   (reduce (partial dissoc-when pred)
           (dissoc-when pred m k)
           ks)))

(defn dissoc-nil
  "Dissocs `k` from `m` if its value is nil.
   
   Works with indices for vectors."
  ([m k]      (dissoc-when nil? m k))
  ([m k & ks] (apply dissoc-when nil? m k ks)))

(defn strip-when
  "Strips each key from `m` for which `pred` called with value returns true.
   
   Does not perform deep traversal."
  [pred m]
  (if-not (map? m)
    #?(:clj  (throw (Exception. "Error: strip-when expects a map."))
       :cljs (do
               (. js/console error "Error: strip-when expects a map.")
               nil))

    (apply dissoc-when pred m (keys m))))

(defn strip-nils
  "Strips each key from `m` which value is nil.
   
   Does not perform deep traversal."
  [m]
  (if-not (map? m)
    #?(:clj  (throw (Exception. "Error: strip-nils expects a map."))
       :cljs (do
               (. js/console error "Error: strip-nils expects a map.")
               nil))

    (apply dissoc-nil m (keys m))))

;; Request helpers
;; ---------------------------------------

(macro/copy-def batchify request/batchify)

;; Response helpers
;; ---------------------------------------

(defn row-to-resource
  [{id :id _st :status rt :resource_type cts :cts ts :ts txid :txid resource :resource :as row}]
  (when (and row (map? row) (or resource {}))
    (-> (cond-> resource
          id (assoc :id id)
          rt (assoc :resourceType rt))
        (update :meta (fn [x]
                        (cond-> (or x {})
                          ts   (assoc :lastUpdated ts)
                          cts  (assoc :createdAt cts)
                          txid (assoc :versionId (str txid))))))))

(defn resources
  "Returns all entries from the data."
  [data]
  (cond
    (empty? data)
    nil

    (:body data)
    (recur (:body data))

    (= "Bundle" (:resourceType data))
    (map :resource (:entry data))

    (sequential? data)
    data

    (contains? data :data)
    (recur (:data data))

    (and (contains? data :entry)
         (not (:resourceType data)))
    (recur (:entry data))

    (map? data)
    (list data)))

(defn data->resource
  "Returns first entry from the data."
  [data]
  (first
   (resources data)))

(defn get-graph-resources
  ;; Authored by r-nikolaev.
  ([graph]
   (get-graph-resources graph []))
  ([graph path & [{:keys [get-resource] :or {get-resource true}}]]
   (letfn [(resource?
             [resource]
             (boolean
              (and (:id resource)
                   (or (:resourceType  resource)
                       (:resource-type resource)
                       (:resource_type resource)))))]
     (loop [value graph
            path  path]
       (cond
         (empty? path)
         (cond
           (map? value)
           (cond-> value
             (and get-resource (resource? value))
             (row-to-resource))

           (sequential? value)
           (keep #(if (and get-resource (resource? %))
                    (row-to-resource %)
                    %) value))

         :else
         (let [path-key (first path)
               value*   (cond (map? value)        (get value path-key)
                              (sequential? value) (reduce (fn [acc v]
                                                            (if (sequential? (path-key v))
                                                              (concat (path-key v) acc)
                                                              (conj acc (path-key v)))) [] value))]
           (recur value*
                  (rest path))))))))

;; Object searches
;; ----------------------------

(defn find-first
  [pred coll]
  (some (fn [x]
          (when (pred x)
            x)) coll))

(defn return-when
  [pred data]
  (when (pred data) data))

(defn code-search
  "Finds code in vector of maps which is equal to one
   of the values provided with descending priority.
   
   Return last found element with equal value."
  [code values coll]
  (let [code* (if (vector? code)
                #(some % code)
                code)]
    (some (into {} (map (juxt code* identity) coll))
          values)))

(defn try-code-search
  "Finds code in vector of maps which is equal to one
   of the values provided with descending priority or
   returns first from the coll.
   
   Return last found element with equal value."
  [code values coll]
  (or (code-search code values coll)
      (first coll)))

(defn codeable-concept-search
  "Finds map in codeable-concept.
   
   Return last found element."
  ([match source] (codeable-concept-search [] match source))
  ([path match source]
   (letfn [(flatten-coll
             [coll]
             (->> coll (map :coding) flatten))]
     (let [source    (get-in source path)
           coll      (if (sequential? source) (flatten-coll source) (:coding source))
           seq-match (if (sequential? match) match (vector match))]
       (some (into {} (map (juxt #(select-keys % (-> seq-match first keys))
                                 identity)
                           coll))
             seq-match))))
  ([path match output source]
   (-> (codeable-concept-search path match source)
       (get output))))

(defn try-codeable-concept-search
  "Finds map in codeable-concept
   or returns first.
   
   Return last found element."
  ([match source]
   (try-codeable-concept-search [] match source))
  ([path match source]
   (or (codeable-concept-search path match source)
       (-> source :coding (first))))
  ([path match output source]
   (-> (try-codeable-concept-search path match source)
       (get output))))

(defn get-by-codeable-concept
  "Filter resources by coding in codeable-concept"
  ([match coll]
   (get-by-codeable-concept [] match coll))
  ([path match coll]
   (find-first (partial codeable-concept-search path match) coll)))

(defn get-coding-from-codeable-concept
  "Returns full coding from codeable-concept"
  [path match source]
  (->> (get-in source path)
       (find-first (partial codeable-concept-search match))))

(defn get-extension
  "Get extension by extension url from any resource"
  ([resource url]
   (->> resource :extension (code-search :url [url])))
  ([resource url output]
   (get (get-extension resource url) output)))

;; ----------------------------

;; Booleans

(defn in?
  "true if coll contains elm"
  [elm coll]
  (boolean (some #{elm} coll)))

(defn intersects?
  "true if some element of one coll meets in the other"
  [a b]
  (some #(contains? (set a) %) b))

(defn contains-in?
  "true if every element of one coll meets in the other"
  [a b]
  (set/subset? (set a) (set b)))

(defn code-between
  "Check that given ICD code between from and to"
  ([code code-group]
   (when (and code code-group)
     (or (= code code-group)
         (and (str/starts-with? code code-group)
              (>= 0 (compare code-group code))))))
  ([code from to]
   (when (and code from to)
     (or (= code from to)
         (and (>= 0 (compare code to))
              (>= 0 (compare from code)))
         (str/starts-with? code from)
         (str/starts-with? code to)))))

;; ----------------------------

(defn flatten-form
  ;; Form as in arbitrary data structure.
  "Flattens form into map of pair `{path value}`.
   
   Stack intensive."
  [form]
  (letfn [(flatten
            [path node]
            (reduce-kv (fn [acc k v]
                         (if-not (map? v)
                           (assoc acc (conj path k) v)
                           (merge acc (flatten (conj path k) v))))
                       {} node))]
    (flatten [] form)))

(defn vectorize
  [x]
  (cond
    ((some-fn seq? set?) x)
    (vec x)

    (vector? x)
    x

    :else
    (vector x)))

(def regex-char-esc-smap
  (let [esc-chars "()*&^%$#![]+"]
    (zipmap esc-chars
            (map #(str "\\" %) esc-chars))))

(defn search-item
  [path search items]
  (let [search' (-> search
                    (str/lower-case)
                    (str/escape regex-char-esc-smap))]
    (filter (fn [item]
              (some->> (get-in item (vectorize path))
                       (str/lower-case)
                       (re-find (re-pattern (str ".*" search' ".*")))))
            items)))

(def gen-uuid core/gen-uuid)

(defn parse-number
  [s]
  (cond
    (re-matches #"[-+]?\d+\.\d+" (str s))
    (parse-num s)

    (re-matches #"[-+]?\d+" (str s))
    (parse-int s)))

(defn token?
  [token]
  (and (string? token)
       (re-matches #".*\|.*" token)))

(defn canonical-reference?
  [reference]
  (and (string? reference)
       (boolean (re-matches #".*/.*" reference))))

(defn direct-reference?
  [reference]
  (boolean
   (and (:resourceType reference)
        (:id reference))))

(defn full-direct-reference?
  [{:keys [resourceType id display]}]
  (every? (complement str/blank?) [resourceType id display]))

(defn make-token
  "Make token from codesystem"
  ([{:keys [value code system] :as codesystem}]
   {:pre  [(or (nil? codesystem) (map? codesystem))
           (or (nil? code) (string? code))
           (or (nil? system) (string? system))
           (or (nil? value) (string? value))]
    :post [(or (string? %) (nil? %))]}
   (cond
     (and (some? code)   (nil? system) (nil? value))  code
     (and (some? system) (nil? code)   (nil? value))  system
     (and (some? system) (some? code)  (nil? value)) (str system "|" code)
     (and (some? system) (nil? code)  (some? value)) (str system "|" value)
     :else nil))
  ([system code]
   (make-token {:system system
                :code   code})))

(defn make-canonical-reference
  ([{:keys [resourceType id] :as reference}]
   {:pre  [(and resourceType id)]
    :post [(string? %)]}
   (str resourceType "/" id))
  ([resourceType id]
   (make-canonical-reference {:resourceType resourceType
                              :id id})))

(defn parse-canonical-reference
  {:doc "Parse canonical resourceType/id to structure {:id id :resourceType resourceType}"}
  ([reference]
   {:pre [(canonical-reference? reference)]}
   (->> (str/split reference #"/")
        (zipmap [:resourceType :id]))))

;; ----------------------------

(defn parse-token
  {:doc "Parse token system|code to structure {:code code :system system}"}
  ([token]
   {:pre [(token? token)]}
   (->> (str/split token #"\|")
        (zipmap [:system :code]))))

(def RECURSIVE-PATH
  (sp/recursive-path [] p
                     (sp/cond-path
                      map?                     (sp/continue-then-stay [sp/MAP-VALS p])
                      sequential?              (sp/continue-then-stay [sp/ALL p])
                      (complement map?)        sp/STAY
                      (complement sequential?) sp/STAY)))

(defn clear-map
  "Removes all empty or nil values from map no matter where they are.
  
   Also removes empty strings."
  [m & [default]]
  (-> (sp/setval [RECURSIVE-PATH
                  (fn [v]
                    (cond (string? v)     (str/blank? v)
                          (sequential? v) (or (empty? v) (nil? v) (every? nil? v))
                          (map? v)        (empty? v)
                          (nil? v)        true))]
                 (or default sp/NONE) m)
      (#(if (= % sp/NONE) nil %))))

(macro/copy-def dissoc-in     core/dissoc-in)
(macro/copy-def get-all-paths core/get-all-paths)
(macro/copy-def dissoc-paths  core/dissoc-paths)
(macro/copy-def assoc-paths   core/assoc-paths)

;; Getters

(defmulti get-identifier-by-type
  (fn [type _] type))

(defmethod get-identifier-by-type :active
  [_ identifiers]
  (->> identifiers
       (some (fn [identifier]
               (when (or
                      (not (get-in identifier [:period :end]))
                      (dh/lt? (dh/current-date) (get-in identifier [:period :end])))
                 identifier)))))

(defmethod get-identifier-by-type :inactive
  [_ identifiers]
  (->> identifiers
       (some (fn [identifier]
               (when (or
                      (not (get-in identifier [:period :end]))
                      (dh/lt? (get-in identifier [:period :end]) (dh/current-date)))
                 identifier)))))

(defmethod get-identifier-by-type :default
  [_ identifiers]
  (first identifiers))

(defn get-identifier
  "Get identifier from any resource"
  ([resource]             (-> resource :identifier first))
  ([resource system]      (get-identifier resource system nil))
  ([resource system type]
   (let [system-set (if (string? system) #{system} (set system))]
     (->> (:identifier resource)
          (filter (comp system-set :system))
          (get-identifier-by-type type)))))

(defn identifier-value
  [& args]
  (->> args
       (apply get-identifier)
       :value))

(defn ratio
  [x y]
  #?(:clj  (format "%.2f" (* 100 (float (try (/ x y) (catch Exception _ 0)))))
     :cljs (format "%.2f" (* 100 (float (try (/ x y) (catch js/Error _ 0)))))))

(defn distinct-by
  "Returns all items in `coll` for which juxt of `fs` returns unique value.
   
   In case of conflicts returns first found item."
  {:arglists '([[f & fs] coll])}
  [fs coll]
  (->> coll
       (group-by (apply juxt fs))
       vals
       (map first)))

(defn count-by
  "Принимает коллекцию и функцию сравнения. 
   Возвращает количество элементов коллекции, для которых функция сравнения вернет true."
  [func coll]
  (->> coll
       (filter func)
       (count)))

(defn num->str
  [num]
  (str/escape (str num) {\. \,}))

(defn code-between-regexp-for-similar-to
  [from to]
  (letfn [(to-group
            [group]
            (str "(" group ")"))
          (next-char
            [char*]
            (cond-> char*
              (not= char* \9)
              (-> (int) (inc) (char))))
          (prev-char
            [char*]
            (cond-> char*
              (not= char* \0)
              (-> (int) (dec) (char))))]
    (let [letter (subs from 0 1)
          [from-prefix from-suffix] (str/split from #"\.")
          [to-prefix to-suffix]     (str/split to #"\.")
          second-from  (nth from-prefix 1)
          third-from   (nth from-prefix 2)
          second-to    (nth to-prefix 1)
          third-to     (nth to-prefix 2)
          left-regexp  (to-group (str from-prefix (when-not (str/blank? from-suffix)
                                                    (str ".[" from-suffix "-9]"))))
          right-regexp (to-group (str to-prefix (when-not (str/blank? to-suffix)
                                                  (str ".[0-" to-suffix "]"))))
          left-regexp-with-subgroups  (to-group (str from-prefix (when-not (str/blank? from-suffix)
                                                                   (str ".[" from-suffix "-9]"))
                                                     ".%"))
          right-regexp-with-subgroups (to-group (str to-prefix (when-not (str/blank? to-suffix)
                                                                 (str ".[0-" to-suffix "]"))
                                                     ".%"))]
      (to-group
       (cond
         (and (= from-prefix to-prefix)
              (str/blank? from-suffix)
              (str/blank? to-suffix))
         (str (to-group from)
              "|"
              (to-group (str from ".%")))
         
         (and (= from-prefix to-prefix)
              (or (not (str/blank? from-suffix))
                  (not (str/blank? to-suffix))))
         (let [regexp* (str from-prefix ".[" (or from-suffix 0) "-" (or to-suffix 9) "]")]
           (str (when (str/blank? from-suffix)
                  (str
                   (to-group from-prefix)
                   "|"))
                (to-group regexp*)
                "|"
                (to-group (str regexp* ".%"))))

         ;; Check if second char in to-prefix is more, than from-prefix
         (pos? (compare second-to second-from))
         (str left-regexp
              "|"
              left-regexp-with-subgroups
              (when-not (= third-from \9)
                (let [sub-regexp-1 (str letter second-from "[" (next-char third-from) "-9]")]
                  (str "|"
                       (to-group sub-regexp-1)
                       "|"
                       (to-group (str sub-regexp-1 ".%")))))
              (when-not (neg? (compare (prev-char second-to) (next-char second-from)))
                ;; Check if second char in to-prefix is more, than from-prefix by 2 or more
                (let [sub-regexp-2 (str letter "[" (next-char second-from) "-" (prev-char second-to) "][0-9]")]
                  (str "|"
                       (to-group sub-regexp-2)
                       "|"
                       (to-group (str sub-regexp-2 ".%")))))
              (when-not (= third-to \0)
                (let [sub-regexp-3 (str letter second-to "[0-" (prev-char third-to) "]")]
                  (str "|"
                       (to-group sub-regexp-3)
                       "|"
                       (to-group (str sub-regexp-3 ".%")))))
              (when-not (str/blank? to-suffix)
                (str "|"
                     (to-group to-prefix)))
              "|"
              right-regexp
              "|"
              right-regexp-with-subgroups)

         (and (= second-from second-to)
              (pos? (compare third-to third-from)))
         (str left-regexp
              "|"
              left-regexp-with-subgroups
              (when-not (neg? (compare (prev-char third-to) (next-char third-from)))
                ;; Check if third char in to-prefix is more, than from-prefix by 2 or more
                (let [sub-regexp-4 (str letter second-from "[" (next-char third-from) "-" (prev-char third-to) "]")]
                  (str "|"
                       (to-group sub-regexp-4)
                       "|"
                       (to-group (str sub-regexp-4 ".%")))))
              (when-not (str/blank? to-suffix)
                (str "|"
                     (to-group to-prefix)))
              "|"
              right-regexp
              "|"
              right-regexp-with-subgroups))))))

(defn url-safe
  [s]
  (some-> s (str/escape {\+ "%2B"})))
