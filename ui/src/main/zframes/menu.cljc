(ns zframes.menu
  (:require [clojure.string :as str]

            [re-frame.core :as rf]

            [common.utils :as cu]

            ["@heroicons/react/24/solid"   :as hi-solid]
            ["@heroicons/react/24/outline" :as hi-outline]))

(defn to-query-params
  [params]
  (->> params
       (map (fn [[k v]] (str (name k) "=" v)))
       (str/join "&")))

(defn href
  [& parts]
  (let [params (if (map? (last parts)) (last parts) nil)
        parts'  (cond-> parts
                  params
                  butlast

                  (= "#" (ffirst parts))
                  (->> rest
                       (cons (-> (first parts)
                                 (subs 1)))))
        url    (->> parts'
                    (map (fn [part]
                           (some-> (cond
                                     (nil? part)
                                     nil

                                     (keyword? part)
                                     (name part)

                                     :else
                                     (str part))
                                   (str/replace #"(^/*)|(/*$)" ""))))
                    (reduce (fn [path part]
                              (str path "/" part))
                            (str (when-not (= "#" (ffirst parts'))
                                   "#"))))]
    (str url (when params (str "?" (to-query-params params))))))

(def ^:private requests-category "Запросы")
(def ^:private domains-category "Домены")

(def map-menus
  "A vector of all the pages that a present in the RMIS. In the nav-bar the items are shown in the same order as they written here, not in the pages-for-roles.

   The item consist of the :id, :href, :display keys. It can also have :items key, if it contains subitems. In this case there is no href in the root item.

   The linked page are represented as vectors that contain them, so be aware of that.
   
   The items can be linked. That way they are always shown together. The linked pages are grouped in the one subvector, and treated as one item."

  [{:id   "home"
    :name "Dashboard"
    :href (href "home")
    :icon hi-solid/HomeIcon}

   {:id   "users"
    :name "Пользователи"
    :href (href "users")
    :icon hi-solid/UserGroupIcon}

   {:id   "domains"
    :name "Домены"
    :href (href "domains")
    :icon hi-solid/GlobeAltIcon}
   
   ;; Категория: Домены
   {:id       "domain_server_providers"
    :name     "Провайдеры серверов"
    :href     (href "domain_server_providers")
    :icon     hi-outline/ServerStackIcon
    :category domains-category}
   {:id       "domain_providers"
    :name     "Провайдеры доменов"
    :href     (href "domain_providers")
    :icon     hi-solid/WrenchScrewdriverIcon
    :category domains-category}

   ;; Категория: Запросы
   {:category requests-category
    :id       "domains_requests"
    :name     "Замена доменов"
    :href     (href "domains" "requests")
    :icon     hi-outline/GlobeAltIcon}])

(def pages-for-roles
  "Для роли должны быть определены страницы, к которым у неё будет доступ.
   При этом отображение меню определяется по id из map-menus, а доступ к странице по первому параметру url.
   Т.е. в случае если id из map-menus не равен первому параметру href, то необходимо прописать оба параметра."
  (->> {"admin"     #{"users" "domains" "domains_requests" "domain_server_providers" "domain_providers"}
        "team_lead" #{"domains"}
        "buyer"     #{}
        "farmer"    #{"home"}}))

(def forbidden-pages
  {"team_lead" 
   [#"^#/[^/]*/.*"]

   "buyer"    
   [#"^#/[^/]*/.*"]

   "farmer"    
   [#"^#/[^/]*/.*"]})

(defn get-main-subitem
  [item]
  (if (vector? item)
    (first item)
    item))

(defn get-menu
  [route]
  (->> (or (:parents route) [])
       (filter :menu)
       (map :menu)
       (last)))

(rf/reg-sub
 ::menu
 :<- [:route-map/current-route]
 (fn [route _]
   (get-menu route)))

(defn fragment-first
  [fragment]
  (some-> fragment
          (str/replace #"^#/" "")
          (str/replace  #"\?(.*)" "")
          (str/split #"/")
          first))

(defn available-tools
  [r]
  (get pages-for-roles r #{"home"}))

(defn get-available-items
  [_ r]
  (->> map-menus
       (mapcat :items)
       (concat (flatten map-menus))
       (filter (every-pred (comp (available-tools r) :id) :href))))

(defn calculate-evs
  [{:keys [fragment] :as db} r]
  (let [fragment-tool   (fragment-first fragment)
        available-items (get-available-items db r)
        default         (first available-items)
        forbidden       (get forbidden-pages r)]
    (cond
      (some #(re-matches % (str fragment)) forbidden)
      [[:flash/error {:header "Доступ запрещен"}]
       [:zframes.redirect/redirect {:uri (:href default)}]]

      (not (some (comp #{fragment-tool} :id) available-items))
      [[:zframes.redirect/redirect {:uri (:href default)}]])))

(defn dispatch-redirect?
  [{:keys [fragment] :as db} r]
  (prn (calculate-evs db r))
  (when (and (not (str/blank? fragment))
             (not (str/blank? r)))
    (calculate-evs db r)))