(ns app.helpers
  (:require [clojure.set    :as set]
            [clojure.walk   :as w]
            [clojure.string :as str]
            [clojure.edn    :as edn]

            [chrono.now      :as now]
            [chrono.core     :as ch]
            [chrono.mask     :as mask]
            [chrono.util     :as util]
            [unit-map.io     :as u.io]
            [unit-map.ops    :as u.ops]

            [re-frame.core        :as rf]
            [re-frame.interceptor :as rfi]

            [com.rpl.specter :as sp]

            [common.routes       :as routes]
            [route-map.core   :as route-map]
            [zframes.routing  :as routing]
            [zframes.menu     :as menu]
            [zframes.mapper   :as zm]
            [app.placeholders :as placeholders]
            [common.utils        :as cu]
            [common.utils.macro  :as cmacro]
            [common.utils.date   :as cud]

            [app.re-frame-helpers]

            #?(:clj [clojure.test :as t])
            #?(:clj [cheshire.core :as json])
            #?(:clj  [clojure.pprint]
               :cljs [cljs.pprint])
            #?@(:cljs [[goog.string :as gstring]
                       [goog.functions]
                       [goog.string.format]]))
  #?(:cljs (:import [goog.async Debouncer])))

(defn vectorize
  [x]
  (cond-> x (not (vector? x)) (vector)))

(defn to-64
  [data]
  #?(:cljs
     (->> data
          clj->js
          (. js/JSON stringify)
          js/encodeURIComponent
          js/btoa)))

(def MAP-MAPS
  (sp/recursive-path [] p
                     (sp/if-path map?
                                 (sp/continue-then-stay sp/MAP-VALS p))))

(def MAP-KEY-WALKER
  (sp/recursive-path [akey] p [sp/ALL (sp/if-path [sp/FIRST #((set akey) %)] sp/LAST [sp/LAST p])]))

(def RECURSIVE-PATH
  (sp/recursive-path [] p
                     (sp/cond-path map?        (sp/continue-then-stay [sp/MAP-VALS p])
                                   sequential? [sp/ALL p]
                                   sp/STAY)))

(defn clear-seq
  "Removes nil values and nullifies collection if all nils"
  [xs]
  (let [result (remove nil? xs)]
    (if (seq result)
      result
      nil)))

(defn clear-map
  "Removes all empty or nil values from map no matter where they are."
  [m & [default]]
  (sp/setval [RECURSIVE-PATH sp/MAP-VALS
              (fn [v]
                (cond (sequential? v) (or (empty? v) (nil? v))
                      (map? v)        (empty? v)
                      (nil? v)        true))]
             (or default sp/NONE) m))

(defn full-clear-map
  "Removes all empty or nil values from map no matter where they are.
  
   Also removes empty strings."
  [m & [default]]
  (sp/setval [RECURSIVE-PATH sp/MAP-VALS
              (fn [v]
                (cond (string? v)     (str/blank? v)
                      (sequential? v) (or (empty? v) (nil? v))
                      (map? v)        (empty? v)
                      (nil? v)        true))]
             (or default sp/NONE) m))

(defn get-value-by-keys
  [m ks]
  (some #(sp/select-first [RECURSIVE-PATH (fn [v] (get v %)) %] m) ks))

(defn ref?
  [m]
  (seq (select-keys m [:id :identifier])))

(defn json-parse-safe
  [x & [{:keys [keywordize-keys]}]]
  (try
    #?(:clj  (json/parse-string x (boolean keywordize-keys))
       :cljs (js->clj (. js/JSON parse x)
                      :keywordize-keys keywordize-keys))
    (catch #?(:cljs js/Error
              :clj com.fasterxml.jackson.core.JsonParseException)
           _
      nil)))

(defn format-str
  [fmt & args]
  (apply
   #?(:clj  clojure.core/format
      :cljs goog.string/format)
   fmt
   args))

(defn pprint
  [x]
  (#?(:clj  clojure.pprint/pprint
      :cljs cljs.pprint/pprint)
   x))

(defn p
  "Pretty prints arg and returns it.
  Useful for debug, especially in ->> and -> marcros'"
  [arg & meta]
  (when meta (apply println meta))
  (pprint arg)
  arg)

(defn camel->kebab
  #?(:clj {:test #(t/is (= "service-request"
                           (camel->kebab "ServiceRequest")))})
  [s]
  (some-> s
          (str/replace
           #"(.?)([A-Z])"
           (fn [[_ prev upper]]
             (str (when-not (str/blank? prev)
                    (str prev \-))
                  (str/lower-case upper))))))

(def rt->kw (comp keyword camel->kebab))

(defn array-map-cons
  [m & kvs]
  (->> (concat (partition 2 kvs) m)
       (apply concat)
       (apply array-map)))

(def trans-table
  {"а" "a",  "б" "b", "в" "v",  "г" "g",  "д" "d",    "е" "e", "ё" "yo", "ж" "zh", "з" "z", "и" "i",  "й" "y"
   "к" "k",  "л" "l", "м" "m",  "н" "n",  "о" "o",    "п" "p", "р" "r",  "с" "s",  "т" "t", "у" "u",  "ф" "f"
   "х" "kh", "ц" "ts" "ч" "ch", "ш" "sh", "щ" "shch", "ь" "",  "ы" "y",  "ъ" "",  "э" "e", "ю" "yu", "я" "ya"
   "А" "A",  "Б" "B", "В" "V",  "Г" "G",  "Д" "D",    "Е" "E", "Ё" "YO", "Ж" "ZH", "З" "Z", "И" "I",  "Й" "Y"
   "К" "K",  "Л" "L", "М" "M",  "Н" "N",  "О" "O",    "П" "P", "Р" "R",  "С" "S",  "Т" "T", "У" "U",  "Ф" "F"
   "Х" "KH", "Ц" "TS" "Ч" "CH", "Ш" "SH", "Щ" "SHCH", "Ь" "",  "Ы" "Y",  "Ъ" "",  "Э" "E", "Ю" "YU", "Я" "YA"})

(def keyword-table
  {"q" "й", "w" "ц", "e" "у", "r" "к", "t" "е", "y" "н", "u" "г", "i" "ш", "o" "щ", "p" "з", "\\[" "х",
   "\\]" "ъ", "a" "ф", "s" "ы", "d" "в", "f" "а", "g" "п", "h" "р", "j" "о", "k" "л", "l" "д", ";" "ж",
   "'" "э", "z" "я", "x" "ч", "c" "с", "v" "м", "b" "и", "n" "т", "m" "ь", "," "б", "." "ю", "`" "ё",
   "Q" "Й", "W" "Ц", "E" "У", "R" "К", "T" "Е", "Y" "Н", "U" "Г", "I" "Ш", "O" "Щ", "P" "З", "{" "Х",
   "}" "Ъ", "A" "Ф", "S" "Ы", "D" "В", "F" "А", "G" "П", "H" "Р", "J" "О", "K" "Л", "L" "Д", ":" "Ж",
   "\"" "Э", "Z" "Я", "X" "Ч", "C" "С", "V" "М", "B" "И", "N" "Т", "M" "Ь", "<" "Б", ">" "Ю", "~" "Ё"})

(defn transliterate
  [cs]
  (str/replace cs
               (re-pattern (str "(?ui)(?:" (str/join "|" (keys trans-table)) ")"))
               (fn [c] (get trans-table c c))))

(defn eng->rus
  [cs]
  (str/replace cs
               (re-pattern (str "(?:" (str/join "|" (keys keyword-table)) ")"))
               (fn [c] (get keyword-table c c))))

(def parseInt
  #?(:clj  (fn [^String s] (Integer/parseInt s))
     :cljs (fn [^String s] (js/parseInt s))))

(def parseFloat
  #?(:clj  (fn [^String s] (Float/parseFloat s))
     :cljs (fn [^String s] (js/parseFloat s))))

(defn parse-int
  [s]
  (when-let [x (re-matches #"[-+]?\d+" (str s))]
    (parseInt x)))

(defn parse-float
  [s]
  (when-let [x (re-matches #"[-+]?\d+(?:\.\d+)?" (str s))]
    (parseFloat x)))

(defn parse-number
  [s]
  (cond
    (re-matches #"[-+]?\d+\.\d+" (str s))
    (parse-float s)

    (re-matches #"[-+]?\d+" (str s))
    (parse-int s)))

(defn gen-uuid
  []
  (str #?(:clj  (java.util.UUID/randomUUID)
          :cljs (random-uuid))))

(defn hmap-to-items
  [m]
  (map (fn [[k v]] {:value k :display v}) m))

(defn get-defaults
  [schema]
  (reduce-kv (fn [acc k v]
               (if-let [v (:default v)]
                 (assoc acc k v)
                 acc))
             {}
             (:fields schema)))

(defn get-in-contained
  [contained m]
  (->> contained
       (filter (comp #{(:localRef m)} :id))
       first))

(defn code-search
  "Finds code in vector of hmaps which is equal to one
   of the values provided with descending prioty.
   
   Return last found element with equal value."
  [code values coll]
  (let [code* (if (vector? code)
                #(some % code)
                code)]
    (some (into {} (map (juxt code* identity) coll))
          values)))

(defn try-code-search
  "Finds code in vector of hmaps which is equal to one
   of the values provided with descending prioty or
   returns first from the coll.
   
   Return last found element with equal value."
  [code values coll]
  (or (code-search code values coll)
      (first coll)))

(defn identity-default
  [default value]
  (if (str/blank? value) default value))

(def identity-default-na
  (partial identity-default placeholders/na-word))

(def identity-default-hyphen
  (partial identity-default placeholders/hyphen-word))

(def identity-default-ellipsis
  (partial identity-default placeholders/ellipsis))

(defn address-without-region
  [str]
  (when str
    (-> str
        (str/replace #"^([0-9]{6}, )?(Чувашия Чувашская Республика -,|Чувашская Республика,|Чувашская Республика - Чувашия, )" "")
        str/trim)))

(defn capitalize-words
  "Capitalize every word in a string"
  [s]
  (->> (str/split (str s) #" ")
       (map str/capitalize)
       (str/join " ")))

(defn parse-fragment
  "Alias of 'zframes.routing/parse-fragment'."
  [fragment]
  (routing/parse-fragment fragment))

(defn to-query-params
  [params]
  (->> params
       (full-clear-map)
       (mapcat (fn [[k v]]
                 (cond
                   (vector? v) (mapv (fn [vv] (str (name k) "=" vv)) v)
                   (set? v)    [(str (name k) "=" (str/join "," v))]
                   :else       [(str (name k) "=" v)])))
       (str/join "&")))

(defn to-url-query
  [url params]
  (str url \? (to-query-params params)))

(defn href
  "Creates fragment-path from parts.
   
   Side-effect: in debug warns if returned href leads to nowhere."
  ^String [& parts]
  (let [suppress-warning?
        (boolean
         (#{:suppress-warning} (last parts)))
        parts  (if suppress-warning?
                 (butlast parts) parts)
        url    (apply menu/href parts)]
    (when (and (not suppress-warning?)
               (not (route-map/match [:. (subs url 1)] routes/routes)))
      (#?(:clj  println
          :cljs (. js/console -warn))
       (str url " has no matching route")))
    url))

(defn href-regress
  "Removes last `n` parts from the `href`."
  ^String #?(:clj   [^Number n ^String href]
             :cljs  [^number n ^String href])
  (when href
    (if-not (#{0} n)
      (recur (dec n)
             (str/replace href #"(?:.(?!/))+$" ""))
      href)))

(defn href-by-route
  ([route-key]
   (routing/ev-short-href route-key))
  ([route-key route-params]
   (routing/ev-short-href route-key route-params)))

(def href-coll (partial apply href))

(defn redirect-to
  [uri & [id]]
  (cond
    id
    (routing/ev-href uri {:id id})

    (keyword? uri)
    (routing/ev-href uri)

    :else
    uri))

(defmacro when-let*
  [bindings & body]
  `(let ~bindings
     (if (and ~@(take-nth 2 bindings))
       (do ~@body))))

(defn href-add
  [href & parts]
  (some-> href
          (->> (re-seq #"[^#/\\]+"))
          vec
          (into parts)
          href-coll))

(defn dissoc-in
  [obj path]
  (cond
    (empty? path)
    obj

    (= (count path) 1)
    (dissoc obj (first path))

    :else
    (update-in obj (drop-last path) dissoc (last path))))

(defn vector-to-hash-map
  [v]
  (if (or (vector? v) (list? v) (map? v))
    (reduce-kv #(assoc %1 %2 (vector-to-hash-map %3)) {} v)
    v))

(defn hash-map-to-vector
  [v]
  (if (map? v)
    (if (every? number? (keys v))
      (reduce-kv #(conj %1 (hash-map-to-vector %3)) [] v)
      (reduce-kv #(assoc %1 %2 (hash-map-to-vector %3)) {} v))
    v))

(defn format-full-name
  [given middle family]
  (let [r (remove nil? [family given middle])]
    (when (seq r) (str/join " " r))))

(defn format-short-name
  [given middle family]
  (let [r (remove nil? [family
                        (when given (str (first given)  "."))
                        (when middle (str (first middle) "."))])]
    (when (seq r) (str/join " " r))))

(defn old->distinct-by
  [fns coll]
  (->> coll
       (group-by (apply juxt fns))
       vals
       (map first)))

(defn distinct-by
  [fns coll]
  (let [key-fn (apply juxt fns)]
    (reduce (fn [acc item]
              (if
               (empty? (filter (fn [acc-item]
                                 (= (key-fn acc-item)
                                    (key-fn item))) acc))
                (cond
                  (list? acc)
                  (conj acc item)

                  (vector? acc)
                  (conj acc item)

                  (map? acc)
                  (merge acc item))
                acc)) (empty coll) coll)))

(defn distinct-by-fn
  [f coll]
  (let [groups (group-by f coll)]
    (map #(first (groups %)) (distinct (map f coll)))))

(defn reg-event-fx-with-ctx-fn
  [ctxs handler]
  (fn [{:keys [db] :as state} [pid phase {:keys [phases] :as params} :as args] & rest-args]
    (let [ctx-id (when phases
                   phase)
          params' (dissoc params :phases)]
      (cond
        (= :deinit phase)
        (apply handler state args rest-args)

        (and (every? (comp #{:done} (partial get (:ctx-statuses db))) ctxs)
             (or (contains? (set ctxs) ctx-id)
                 (nil? ctx-id)))
        (cond
          phases
          {:fx (mapv (fn [phase]
                       [:dispatch [pid phase params']])
                     phases)}

          (#{:init :params} phase)
          (apply handler state (assoc args 1 phase) rest-args)

          :else
          (apply handler state args rest-args))

        (and ctx-id
             (not (contains? (set ctxs) ctx-id)))
        {:zframes.routing/skip :ctx-not-required}

        (not-every? (comp #{:done} (partial get (:ctx-statuses db))) ctxs)
        {:zframes.routing/skip :ctx-not-ready}))))

(defn reg-event-fx-with-ctx
  "Blocks `handler` evaluation until after all given `ctxs` has been set into :done status. Do not resolves contexts itself.
   
   Contexts are set in common.routes and resolved in zframes.routing.
   
   :app.global-context/organization will be resolved on every page."
  ([pid ctxs handler]
   (rf/reg-event-fx pid (reg-event-fx-with-ctx-fn ctxs handler)))
  ([pid ctxs interceptors handler]
   (rf/reg-event-fx pid interceptors (reg-event-fx-with-ctx-fn ctxs handler))))

(rf/reg-event-fx
 ::redirect
 (fn [_ [_ uri params]]
   (let [redirect-uri (cond
                        (map? params)
                        (routing/ev-href uri params)

                        params
                        (routing/ev-href uri {:id params})

                        (keyword? uri)
                        (routing/ev-href uri)

                        :else
                        uri)]
     {:dispatch [:zframes.redirect/redirect
                 (cond-> {:uri redirect-uri})]})))

(rf/reg-event-fx
 ::redirect-to
 (fn [_ [_ uri & [params]]]
   (let [redirect-uri (routing/ev-short-href uri params)]
     {:dispatch [:zframes.redirect/redirect {:uri redirect-uri}]})))

(rf/reg-event-db
 :xhr/loaded
 (fn [db [_ {:keys [data]} {:keys [pid key]}]]
   (-> db
       (assoc-in [pid :loading] false)
       (assoc-in [pid key]      data))))

(rf/reg-event-db
 :xhr/failed
 (fn [db [_ _ {:keys [pid]}]]
   (-> db
       (assoc-in [pid :loading] false)
       (assoc-in [pid :status] :error))))

(rf/reg-event-fx
 ::deinit-page
 (fn [{db :db} [_ pid]]
   {:dispatch [:xhr/deinit-page pid]
    :db       (dissoc db pid)}))

(defn redirect-search
  ([search]
   (rf/dispatch [:zframes.redirect/set-params {:search search}]))
  ([params search]
   (rf/dispatch [:zframes.redirect/set-params (if (str/blank? search)
                                                (dissoc params :search)
                                                (assoc params :search search))])))

(defn patch
  [schema mappings old-resource new-resource]
  (let [empty-data         {:collection []
                            :string     ""
                            :object     {}}
        empty-form         (into {} (map (fn [[field props]] {field (get empty-data (:type props))})
                                         (:fields schema)))
        form-resource-keys (keys (zm/export empty-form mappings))]
    (merge (apply dissoc old-resource
                  :meta
                  (set/difference (set form-resource-keys)
                                  (set (keys new-resource))))
           new-resource)))

(def date-fmt
  {:iso {:date           [:year "-" :month "-" :day]
         :datetime       [:year "-" :month "-" :day "T" :hour ":" :min ":" :sec]
         :datetime-short [:year "-" :month "-" :day "T" :hour ":" :min]}
   :ru  {:date           [:day "." :month "." :year]
         :datetime       [:day "." :month "." :year " " :hour ":" :min ":" :sec]
         :datetime-short [:day "." :month "." :year " " :hour ":" :min]}})

(def time-fmt [:hour ":" :min ":" :sec])

(def iso-fmt [:year "-" :month "-" :day "T" :hour ":" :min ":" :sec])

;; TODO: this is temporary solution
(def chuvash-tz-iso-fmt [:year "-" :month "-" :day "T" :hour ":" :min ":" :sec "+03:00"])

(def ru-fmt-with-time [:day "." :month "." :year " " :hour ":" :min ":" :sec])

(def start-time {:hour 0 :min 0 :sec 0})
(def end-time {:hour 23 :min 59 :sec 59})

(defn now-date-without-time
  []
  (ch/format (now/local) (get-in date-fmt [:iso :date])))

(defn now-datetime
  []
  (ch/format (now/local) chuvash-tz-iso-fmt))

(defn now-date-end
  []
  (ch/format (merge (now/local) end-time) iso-fmt))

(defn prev-month-date-without-time
  []
  (ch/format (ch/+ (select-keys (now/local) [:year :month :day]) {:month -1}) (get-in date-fmt [:iso :date])))

(comment
  #?(:cljs (let [current (js/Date.) ;see next comment block
                 prev-month (js/Date. (. current setMonth (- (. current getMonth) 1)))]
             (some->> (clojure.string/split (. prev-month toLocaleDateString) #"\.")
                      reverse
                      (clojure.string/join "-")))
     :clj (ch/format (ch/+ (select-keys (now/local) [:year :month :day]) {:month -1}) (get-in date-fmt [:iso :date])))

  (ch/format (ch/+ (select-keys (now/local) [:year :month :day]) {:month -1}) (get-in date-fmt [:iso :date]))

  (ch/format (ch/+ (now/local) {:month -1}) (get-in date-fmt [:iso :date]))

  (= "2019-12-13" (ch/format (ch/+ (select-keys (now/local) [:year :month :day]) {:month -1}) (get-in date-fmt [:iso :date]))))

(defn prev-day-date-without-time
  []
  (ch/format (ch/+ (now/local) {:day -1}) (get-in date-fmt [:iso :date])))

(defn prev-day-date
  []
  (ch/format (ch/- (now/local) {:day 1}) iso-fmt))

(defn tomorrow-date-without-time
  []
  (ch/format (ch/+ (now/local) {:day +1}) (get-in date-fmt [:iso :date])))

(defn tomorrow-date
  []
  (ch/format (ch/+ (now/local) {:day 1}) iso-fmt))

(defn ru-fmt-date-with-time
  []
  (ch/format (now/local) ru-fmt-with-time))

(defn now-date
  []
  (ch/format (now/local) iso-fmt))

(defn now-time
  []
  (ch/format (now/local) time-fmt))

(defn datetime-iso-format
  "Pattern like YYYY-MM-DDTHH:mm:ss"
  [pattern value]
  (if (or (str/blank? value) (str/blank? pattern))
    nil
    (let [t (ch/parse value iso-fmt)]
      (-> pattern
          (str/replace-first #"YYYY" (str (:year t)))
          (str/replace-first #"MM"   (format-str "%02d" (:month t)))
          (str/replace-first #"DD"   (format-str "%02d" (:day t)))
          (str/replace-first #"HH"   (format-str "%02d" (:hour t)))
          (str/replace-first #"mm"   (format-str "%02d" (:min t)))
          (str/replace-first #"ss"   (format-str "%02d" (:sec t)))))))

(defn date-iso->rus-format
  [value]
  (when value
    (ch/format (ch/parse value iso-fmt) (get-in date-fmt [:ru :date]))))

(defn date-iso->rus-format-with-time
  [value]
  (when value
    (ch/format (ch/parse value iso-fmt) ru-fmt-with-time)))

(defn date-rus->iso-format
  [value]
  (when value
    (ch/format (ch/parse value (get-in date-fmt [:ru :date])) (get-in date-fmt [:iso :date]))))

(defn date-chrono->iso-format
  [value]
  (when value
    (ch/format value (get-in date-fmt [:iso :date]))))

(defn date-iso->chrono-format
  [value]
  (when value
    (ch/parse value (get-in date-fmt [:iso :date]))))

(defn datetime-chrono->iso-format
  [value]
  (when value
    (ch/format value iso-fmt)))

(defn appointment-day
  [date]
  (some->> date
           (re-find #"^(\d{4})-(\d{2})-(\d{2})")
           rest
           vec
           (#(update % 0 subs 2 4))
           reverse
           (str/join ".")))

(def data-mask-get date-iso->rus-format)

(def ru-full-date date-iso->rus-format)

(def dmY-Hm date-iso->rus-format)

(defn full-date-post
  [date]
  (when date
    (ch/format (ch/parse  date
                          [:day "." :month "." :year " " :hour ":" :min])
               [:year "-" :month "-" :day "T" :hour ":" :min])))

(defn full-date-get
  "DEPRECATED. Use `common.utils.date` instead."
  [date]
  (when date
    (ch/format (ch/parse  date
                          [:year "-" :month "-" :day "T" :hour ":" :min])
               [:day "." :month "." :year " " :hour ":" :min])))

(defn date-without-time
  "DEPRECATED. Use `common.utils.date` instead."
  [date]
  (when date
    (ch/format (ch/parse date
                         [:year "-" :month "-" :day])
               [:day "." :month "." :year])))

(defn data-mask-post
  [date]
  (when date
    (ch/format (ch/parse date (get-in date-fmt [:ru :date])) (get-in date-fmt [:iso :date]))))

(defn date-iso->rus-datetime-format
  "DD.MM.YYYY hh:mm:ss
   
   DEPRECATED. Use `common.utils.date` instead."
  [value]
  (ch/format (ch/parse value)
             [:day "." :month "." :year " " :hour ":" :min ":" :sec]))

(def rus-date          date-iso->rus-format)
(def ru-datetime       date-iso->rus-datetime-format)
(def ru-datetime-short full-date-get)

(defn rus-datetime->date-iso-format
  [value]
  (ch/format (ch/parse value
                       [:day "." :month "." :year " " :hour ":" :min ":" :sec])
             [:year "-" :month "-" :day "T" :hour ":" :min ":" :sec]))

(defn iso-date
  "DEPRECATED. Use common.utils.date instead."
  [date]
  (ch/format (ch/parse date) (get-in date-fmt [:iso :date])))

(defn iso-datetime
  "DEPRECATED. Use common.utils.date instead."
  [date]
  (ch/format (assoc (ch/parse date) :sec 0)
             [:year \- :month \- :day \T :hour \: :min \: :sec]))

(defn iso-datetime-short
  "DEPRECATED. Use common.utils.date instead."
  [date]
  (ch/format (ch/parse date) (get-in date-fmt [:iso :datetime-short])))

(defn day*
  [date]
  (ch/format (ch/parse date) [:day]))

(def today
  "DEPRECATED. Use common.utils.date instead."
  (now/today))

(defn iso-today
  "DEPRECATED. Use common.utils.date instead."
  []
  (ch/format (now/today) (get-in date-fmt [:iso :date])))

(defn now
  []
  (ch/format (now/local) [:day \. :month \. :year]))

(defn iso-date-today
  []
  (ch/format (now/local) [:year \- :month \- :day]))

(defn iso-datetime-today
  []
  (ch/format (now/local) [:year \- :month \- :day \T :hour \: :min \: :sec]))

(defn iso-datetime-prev-month
  []
  (ch/format (ch/+ (now/local) {:month -1}) iso-fmt))

(defn time*
  [date]
  (ch/format (or (ch/parse date) (mask/parse date util/iso-fmt)) [:hour \: :min]))

(defn dmY-Hms
  [date]
  (ch/format (ch/parse date) [:day \. :month \. :year \space :hour \: :min]))

(defn vec->json-path
  [v]
  (str \{ (str/join \, v) \}))

(defn vec->str
  [v & [separator]]
  (when (sequential? v)
    (cond->> (remove nil? v)

      separator
      (str/join separator))))

(do
  #?@(:cljs [(defn ru-date
               [date]
               (. (js/moment date) format "DD MMMM "))

             (defn ru-date-with-year
               [date]
               (. (js/moment date) format "DD MMMM YYYY"))

             (defn slot-time
               [date]
               (. (js/moment date) format "dd. DD.MM в HH:mm"))

             (defn slot-accept-time
               [date]
               (. (js/moment date) format "DD.MM.YYYY (dd) HH:mm"))

             (defn walkin-slot-accept-time
               [date]
               (. (js/moment date) format "DD.MM.YYYY (dd)"))

             (defn day-of-year
               [date]
               (. (js/moment date) format "DDD"))

             (defn first-day-of-week
               []
               (. (js/moment.) startOf "isoweek"))

             (defn month-text
               ([]  (. (js/moment.) format "MMMM YYYY"))
               ([d] (. d format "MMMM YYYY")))

             (defn month
               ([]  (. (js/moment.) format "YYYY MM"))
               ([d] (. d format "YYYY MM")))

             (defn first-day-of-month-on-calendar
               ([]  (. (. (js/moment.) startOf "month") startOf "isoweek"))
               ([d] (. (. d startOf "month") startOf "isoweek")))

             (defn add-interval
               [from c d]
               (. (js/moment from) add c d))

             (defn remove-interval
               [from c d]
               (. (js/moment from) add (* -1 c) d))

             (defn next-month-from
               [from]
               (add-interval from 1 "M"))

             (defn prev-month-from
               [from]
               (remove-interval from 1 "M"))]))

(defn leader-zero
  [s]
  (if-let [n (parse-int s)]
    #?(:cljs (gstring/format "%02d" n)
       :clj (format "%02d" n))
    s))

(defn float-to-str
  [s & [l]]
  #?(:cljs (gstring/format "%.1f" s)
     :clj (format "%.1f" s)))

(defn humanize-size
  [size]
  (if (> size (/ (* 1024 1024) 10))
    (some-> size (/ 1024) (/ 1024) (float) (float-to-str) (str " Mb"))
    (some-> size (/ 1024) (float) (float-to-str) (str " Kb"))))

(defn yearyfy
  [d]
  (if (and (= 3 (count d)) (= 2 (count (nth d 2))))
    (conj (vec (butlast d)) (str "20" (last d)))
    d))

(defn try-iso-inverse
  [s]
  (let [s (or s "")]
    (if (or (re-matches #"(?:\d{1}|\d{2})\.(\d{4}|\d{2}|\d{1})"  s)
            (re-matches #"(?:\d{1}|\d{2})\.(?:\d{4}|\d{2}|\d{1})\.(?:\d{2})" s)
            (re-matches #"(?:\d{1}|\d{2})\.(?:\d{4}|\d{2}|\d{1})\.(?:\d{4})" s))
      (some->
       s
       (str/split #"\.")
       (->> (map leader-zero))
       yearyfy
       reverse
       (->> (str/join "-")))
      s)))

(defn try-format-ru-dates-to-iso-in-search
  [s]
  (some->
   s
   (str/split #"\ ")
   (->>
    (map #(str/replace % #"(\.0|\.)$" ""))
    (map try-iso-inverse)
    (str/join " "))))

(defn search-line-format
  [line]
  (some-> line
          try-format-ru-dates-to-iso-in-search
          str/trim
          (str/replace #"000000" "")))

(comment
  (search-line-format "марат 03.12")
  (search-line-format "03.12.19")
  (search-line-format "marat 2021-02-01"))

(defn deep-merge
  "efficient deep merge"
  [a b]
  (loop [[[k v :as i] & ks] b
         acc a]
    (if (nil? i)
      acc
      (let [av (get a k)]
        (if (= v av)
          (recur ks acc)
          (recur ks (if (and (map? v) (map? av))
                      (assoc acc k (deep-merge av v))
                      (assoc acc k v))))))))

(defn deep-merge-concat
  [a b]
  (merge-with
   (fn [x y]
     (cond (map? y)        (deep-merge x y)
           (sequential? y) (into x y)
           :else           y))
   a b))

(def search
  #?(:clj redirect-search
     :cljs (let [debouncer (Debouncer. redirect-search 400)]
             (fn [e] (->> e .-target .-value (. debouncer fire))))))

(defn debounce
  [f interval]
  #?(:cljs (goog.functions/debounce f interval)
     :clj  f))

(defn practitionerrole-display
  [prr]
  (str/join " " (concat [(get-in prr [:derived :name 0 :family])]
                        (get-in prr [:derived :name 0 :given])
                        [(str "(" (get-in prr [:code 0 :text]) ")")]
                        (when-let [emp (get-in prr [:employment :display])]
                          [emp]))))

(defn practitioner-display
  [pr]
  (str (str/join " " (concat [(get-in pr [:name 0 :family])]
                             (get-in pr [:name 0 :given])))
       ", "
       (ru-full-date (:birthDate pr))))

(defn user-display
  [resource]
  (->> resource
       :name
       ((juxt :familyName :givenName :middleName))
       (str/join " ")))

(defn resources-by-type
  [{:keys [data]} resource-type]
  (if (= "Bundle" (:resourceType data))
    (->> data
         :entry
         (filter #(= (-> % :resource :resourceType) resource-type))
         (map :resource))
    (list data)))

(def period-format
  (comp (partial str/join  " - ")
        (partial map identity-default-na)
        (juxt (comp date-iso->rus-format :start)
              (comp date-iso->rus-format :end))))

(def format-period
  (comp (partial str/join  " — ")
        (partial map identity-default-ellipsis)
        (juxt (comp date-iso->rus-format :start)
              (comp date-iso->rus-format :end))))

(defn fnotnil
  [f]
  (fn [x]
    (when x
      (f x))))

(defn not-blank
  ([attr content]
   (when-not (str/blank? attr)
     content))
  ([attr] (not-blank attr attr)))

(defn vec-remove
  "remove elem in coll
  source: https://stackoverflow.com/a/18319708"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn remove-displays
  #?(:clj
     {:test
      #(->> {:x 1, :display :foo
             :xs [{:x 2, :display :foo}
                  {:x 3, :display :foo}]}
            remove-displays
            (= {:x 1 :xs [{:x 2} {:x 3}]})
            t/is)})
  [m]
  (w/postwalk #(cond-> %
                 (and (map? %) (:display %))
                 (dissoc :display))
              m))

(defn seqs->sets
  #?(:clj
     {:test
      #(->> {:x 1, :display :foo
             :xs [{:x 2, :display :foo}
                  {:x 3, :display :foo}]}
            seqs->sets
            (= {:x 1, :display :foo
                :xs #{{:x 2, :display :foo}
                      {:x 3, :display :foo}}})
            t/is)})
  [m]
  (w/postwalk
   #(cond-> %
      (and (sequential? %) (not (map-entry? %)))
      set)
   m))

(defn form-error-message
  [errors]
  [:<>
   (map-indexed
    (fn [idx [_ value]]
      ^{:key idx}
      [:div (vals value)])
    errors)])

(def index-search
  #?(:clj #(rf/dispatch [:zframes.redirect/merge-params {:search %}])
     :cljs (let [debouncer (Debouncer. #(rf/dispatch [:zframes.redirect/merge-params {:search %}]) 300)]
             (fn [e] (->> e .-target .-value (. debouncer fire))))))

(rf/reg-event-fx
 ::set-filter-params
 (fn [_ [_ field-value _ field-path param field]]
   {:dispatch [:zframes.redirect/merge-params (hash-map (or param (last field-path))
                                                        (or (when field (get-in field-value field))
                                                            (:id field-value)
                                                            (:value field-value)
                                                            field-value))]}))

(defn flatten-keys*
  [a ks m]
  (cond
    (map? m)        (reduce into (map (fn [[k v]] (flatten-keys* (assoc a ks m) (conj ks k) v)) (seq m)))
    (sequential? m) (reduce into (map (fn [[k v]] (flatten-keys* a (conj ks k) v)) (map-indexed vector m)))
    :else           (assoc a ks m)))

(defn flatten-keys
  [m]
  (flatten-keys* {} [] m))

(defn find-paths
  ([k o]
   (find-paths k identity o))
  ([k pred o]
   (find-paths k pred [] [nil o]))
  ([k pred path [pk o]]
   (let [path (cond-> path pk (conj pk))]
     (cond
       (and (= pk k) (pred o)) [path]
       (coll? o) (->> (cond->> o (not (map? o)) (map-indexed vector))
                      (mapcat (partial find-paths k pred path))
                      (remove nil?))))))

(defn nested-find
  [k pred o]
  (cond
    (and (map? o) (pred (get o k))) (get o k)
    (coll? o) (some (partial nested-find k pred) o)))

(defn nested-search
  [pred o]
  (cond
    (and (map? o) (pred o)) o
    (coll? o) (some (partial nested-search pred) o)))

(defn nested-find-all
  [k pred o]
  (cond
    (and (map? o) (pred (get o k))) [(get o k)]
    (coll? o) (->> o
                   (mapcat (partial nested-find-all k pred))
                   (remove empty?))))

(defn single-elem?
  [s]
  (and (seq s) (empty? (rest s))))

(defn in?
  "true if coll contains elm"
  [elm coll]
  (boolean (some #{elm} coll)))

(def vector-map?
  "Check if a map is vector converted to map.
   e.g. [6 7 8] => {0 6, 1 7, 2 8}"
  (every-pred map? (comp (partial every? int?) keys)))

(defn pluralize
  "Return the pluralized noun from given forms"
  ([num [form1 form2 form3]]
   (let [n  (rem num 100)
         n1 (rem num 10)]
     (cond
       (< 10 n 20) form3
       (< 1 n1 5) form2
       (= n1 1) form1
       :else form3)))
  ([num forms full?]
   (when full? (str num " " (pluralize num forms)))))

(defn assoc-when
  ([pred m k v]
   (cond-> m (pred v) (assoc k v)))
  ([pred m k v & kvs]
   {:pre [(even? (count kvs))]}
   (reduce (partial apply assoc-when pred)
           (assoc-when pred m k v)
           (partition 2 kvs))))

(defn assoc-some
  ([m k v]       (assoc-when some? m k v))
  ([m k v & kvs] (apply assoc-when some? m k v kvs)))

(defn dissoc-when
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
  ([m k]      (dissoc-when nil? m k))
  ([m k & ks] (apply dissoc-when nil? m k ks)))

(defn strip-when
  [pred m]
  (apply dissoc-when pred m (keys m)))

(defn strip-nils
  [m]
  (apply dissoc-nil m (keys m)))

(defn fmt-query-string
  [params]
  (->> params
       (map (fn [[k v]] (str (name k) \= v)))
       (str/join "&")))

(defn code-from-coding
  ([coding]
   (->> coding :coding first :code))
  ([coding system]
   (->> coding :coding (code-search :system system) :code)))

(defn def-pid-reg-sub
  [pid arg1 & [arg2]]
  (if arg2
    (rf/reg-sub pid arg1
                (comp (partial hash-map pid) arg2))
    (rf/reg-sub pid
                (comp (partial hash-map pid) arg1))))

(defn add-form-events
  [events schema]
  (update schema :form-schema
          (fn [form-schema]
            (reduce
             (fn [accumulator [path value]]
               (update-in accumulator path merge value))
             form-schema events))))

(defn left-join
  "Merge vector of maps by same value.
   
   https://stackoverflow.com/questions/22097064/ "
  [key-map xs ys]
  (let [kes (seq key-map)
        lks (mapv key kes)
        rks (mapv val kes)
        gxs (group-by #(mapv (fn [k] (get % k)) lks) xs)
        gys (dissoc (group-by #(mapv (fn [v] (get % v)) rks) ys) nil)
        kvs (keys gxs)]
    (persistent!
     (reduce (fn [out kv]
               (let [l (get gxs kv)
                     r (get gys kv)]
                 (if (seq r)
                   (reduce (fn [out m1]
                             (reduce (fn [out m2]
                                       (conj! out (merge m1 m2)))
                                     out
                                     r))
                           out
                           l)
                   (reduce conj! out l))))
             (transient [])
             kvs))))

(defn combine
  "Return seq of vectors containing combination of `colls`."
  [& colls]
  (reduce (fn [acc coll]
            (if (seq coll)
              (mapcat (fn [el]
                        (map (fn [coll]
                               (conj coll el))
                             acc))
                      coll)
              (map (fn [coll]
                     (conj coll nil))
                   acc)))
          '([]) colls))

(defn find-first
  [pred coll]
  (some (fn [x]
          (when (pred x)
            x)) coll))

(defn map-sort
  "Sort hash-map by specified order in vector"
  [m order]
  (let [order-map (apply hash-map (interleave order (range)))]
    (conj
     (sorted-map-by #(compare (order-map %1) (order-map %2)))
     (select-keys m order))))

(defn last-by-master-idf
  [coll]
  (->> coll
       (sort-by #(-> % :masterIdentifier :value
                     (str/split #"/")
                     (second)
                     (str/split #"-")
                     ((juxt first second))))
       (last)))

(defmacro spy-env
  []
  (let [ks (keys &env)]
    `(prn (zipmap '~ks [~@ks]))))

(defn maskify
  [v & [mask-type]]
  (case mask-type
    :passport (some-> v
                      (str/replace #":" " № "))
    (case (count v)
      (9 14) (->> v (re-seq #".{1,3}") (str/join " "))
      16     (->> v (re-seq #".{1,4}") (str/join " "))
      v)))

(defn canonical-ref
  [resource]
  (str (:resourceType resource) "/" (:id resource)))

(rf/reg-event-fx
 ::errored
 (fn [_ [_ resp]]
   (prn (:response resp))))

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

(defn recursive-replace
  [s replacements]
  (reduce (fn [acc {pattern :pattern replacement :replacement}]
            (str/replace acc pattern replacement))
          s replacements))

(defn flatten-by
  [key-fn coll]
  (loop [items coll]
    (if (->> items (filter key-fn) seq)
      (->> items
           (reduce (fn [acc item]
                     (into acc (key-fn item))) [])
           (into (map #(dissoc % key-fn) items))
           recur)
      items)))

(defn rename-keys
  [coll & ks]
  (->> (partition 2 ks)
       (reduce (fn [acc [old new]]
                 (-> acc
                     (assoc new (old acc))
                     (dissoc old)))
               coll)))

(defn get-top
  [node & [root top]]
  (let [top (or top 0)]
    (letfn [(get-top*
              [node & [root top]]
              (if (and (some? (. node -offsetParent))
                       (not= root node))
                (let [client-top (or (. node -clientTop) 0)
                      offset-top (. node -offsetTop)]
                  (+ top client-top offset-top (get-top* (. node -offsetParent) root top)))
                top))]
      (get-top* node root top))))

(defn get-bottom
  [node & [root top]]
  (+ (get-top node root top)
     (. node -clientHeight)))

(def week-days ["mon" "tue" "wed" "thu" "fri" "sat" "sun"])

(defn week-day-number->name
  [number]
  (get week-days number))

(defn split
  "An alias for 'str/split' with arguments swapped. Useful for threading."
  [re s]
  (str/split s re))

(def debug
  (rfi/->interceptor
   :id     :debug
   :before (fn [context]
             (tap> (rfi/get-coeffect context :event))
             context)
   :after  identity))

(defn success-event
  ([{:keys [success data]}]
   (success-event success data))
  ([success data]
   [(:event success) (assoc (:params success) :data data)]))

(defn success-with-data
  ([{:keys [success data]}]
   (success-with-data success data))
  ([success data]
   (assoc-in success [:params :data] data)))

(defn get-newest-identifier
  [code values identifiers]
  (->> identifiers
       (filter (fn [item] (values (code item))))
       (sort-by #(-> % :period :start) cud/compare-date)
       (first)))

(defn ln
  [x]
  (when-let [x* (parse-number x)]
    (Math/log x*)))

(defn normalize-styles
  [hiccup-html]
  (if-not (keyword? (first hiccup-html))
    hiccup-html
    (reduce
     (fn [acc el]
       (cond
         (and (map? el) (string? (:style el)))
         (conj acc (update el :style (fn [styles]
                                       (-> styles
                                           (str/split #";")
                                           (->> (map #(str/split % #":")))
                                           (flatten)
                                           (->> (apply hash-map))
                                           (w/keywordize-keys)))))

         (vector? el)
         (conj acc (normalize-styles el))

         :else
         (conj acc el)))
     []
     hiccup-html)))

(defn get-name-and-ext
  [file]
  (let [[name ext] (->> (. file -name)
                        (#(str/split % #"\."))
                        ((fn [parts]
                           [(str/join "." (butlast parts)) (str "." (last parts))])))]
    {:name name
     :ext  ext}))

(defn make-id
  [path]
  (letfn [(make-part
            [part]
            (cond
              (keyword? part)
              (name part)

              (integer? part)
              (str part)

              :else
              part))]
    (->> (cond->> path
           (not (re-matches #"[A-Za-z].*" (make-part (first path))))
           (cons "id")) ; That way guarantied that id starts with a letter.
         (map make-part)
         (str/join "-")
         ((cu/rpartial str/escape {\. ""})))))