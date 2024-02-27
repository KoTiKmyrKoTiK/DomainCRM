(ns zframes.xhr
  (:require [clojure.string :as str]

            [re-frame.db    :as db :refer [app-db]]
            [re-frame.core  :as rf]
            [re-frame.utils :refer [dissoc-in]]

            [com.rpl.specter :as sp]

            [common.utils :as cu]

            [zframes.auth :as  auth]

            [cljs.core.async         :refer [go <!]]
            [cljs.core.async.interop :refer-macros [<p!]]

            [app.helpers :as h]))

(defn file-response
  ([resp]
   (let [reader (-> resp .-body .getReader)
         contentLength (-> resp .-headers (.get "Content-Length"))]
     (file-response reader contentLength 0)))
  ([reader contentLength receivedLength]
   (-> (.read reader)
       (.then
        (fn [doc]
          (when-not (.-done doc)
            (let [length (-> doc .-value .-length)]
              (.log js/console (.-done doc))
              (.log js/console (str "Received " receivedLength " of " contentLength))
              (file-response reader contentLength (+ receivedLength length))))))
       (.catch (fn [_] nil)))))

(defn base-url
  [db url]
  (str (get-in db [:config :base-url]) url))

(defn make-form-data
  [files]
  (let [form-data (js/FormData.)]
    (doall
     (for [[i file] (map-indexed vector files)]
       (.append form-data (str "file" i) file (str "file" i))))
    form-data))

(defonce abort-controller-cache (atom {}))

(defn get-abort-controller
  [req-id]
  (when-let [ctrl (get @abort-controller-cache req-id)]
    (.abort ctrl))
  (swap! abort-controller-cache assoc req-id (js/AbortController.))
  (get @abort-controller-cache req-id))

(defn- prepare-fetch-opts
  [{:keys [req-id format headers without-auth files] request-body :body :as opts}
   role]
  (let [{:keys [x-correlation-id]}
        (get-in @app-db [:xhr :config])

        abort-controller
        (when req-id (get-abort-controller req-id))

        fmt
        (case format
          "json" "application/json"
          "yaml" "text/yaml"

          "application/json")

        headers'
        (cond-> {"accept" fmt}

          (nil? files)
          (assoc "Content-Type" "application/json")

          headers
          (merge headers))]
    (cond-> (merge {:method      "get"
                    :mode        "cors"
                    :credentials "same-origin"}
                   opts)

      abort-controller
      (assoc :signal (.-signal abort-controller))

      request-body
      (assoc :body (if (string? request-body)
                     request-body
                     (->> (clj->js request-body)
                          (.stringify js/JSON))))

      files
      (assoc :body (make-form-data files))

      :always
      (-> (dissoc :uri :success :error :params :files)
          (assoc :headers headers')))))

(defn error-flash-view
  [pid base-url url resp x-correlation-id data]
  (when resp
    (let [status      (.-status resp)
          status-text (.-statusText resp)
          errors      (->> data :issue
                           (map (fn [err]
                                  (->> [(:expression err) (:diagnostics err)]
                                       (filter some?)
                                       (str/join " - ")))))
          err-text    (when (string? data)
                        (subs data 0
                              (if (> (count data) 1000)
                                1000
                                (count data))))]
      [:flash/danger
       {:msg
        [:div
         (cond
           (some (comp #{"forbidden"} :code) (:issue data))
           [:div "Ошибка: " [:b "403"] " Доступ запрещен."]

           (:message data)
           [:<>
            [:div "Ошибка: " [:b status]]
            [:div (:message data)]]

           :else
           [:<>
            [:div "Ошибка: " [:b status] " " status-text]
            (case status
              400 [:div "Неверный запрос"]
              403 [:div "Доступ запрещен"]
              404 [:div "Неверный адрес запроса: " url]
              408 [:div "Превышено время ожидания ответа"]
              409 [:div "Конфликт с текущим состоянием сервера"]
              422 [:div "Ошибка при обработке запроса"]
              500 [:div "Внутренняя ошибка сервера"]
              502 [:div "Не удалось установить соединение"]
              [:div "Неопознанная ошибка"])])
         (when (and (seq errors) (#{400 422} status))
           (into [:ul]
                 (for [e errors] [:li e])))
         [:div
          [:div.btn-sm.btn.mt-2.btn-outline-secondary.btn-block
           {:title    "Отправить отчет об ошибке"
            :on-click #(rf/dispatch
                        [:xhr/error-report
                         {:msg (str "------------------------------\n"
                                    "<b>Error report:</b> Status " status "\n"
                                    "<b>Instance:</b> " base-url "\n"
                                    "<b>Screen:</b> " pid "\n"
                                    "<b>Req url:</b> " url "\n"
                                    "<b>Correlation-id:</b> " x-correlation-id "\n"
                                    "<pre><code>"
                                    (if data
                                      (str/replace (.stringify js/JSON (clj->js data)) #"<" "меньше")
                                      err-text)
                                    "</code></pre>")}])}
           "Сообщить об ошибке"]]]}])))

(defn get-response-body
  [resp]
  (go
    (try
      (let [content-type
            (some-> (.-headers resp)
                    (.get "Content-Type"))]
        (if (and content-type (str/includes? content-type "application/json"))
          (-> (.json resp)
              (<p!)
              (js->clj :keywordize-keys true))
          (<p! (.text resp))))
      (catch js/Error _ {}))))

(defn *json-fetch
  [{:keys [uri params success error] :as opts}
   role]
  (let [fetch-opts       (prepare-fetch-opts opts role)
        x-correlation-id (get-in @app-db [:xhr :config :x-correlation-id])
        pid              (get-in @app-db [:route-map/current-route :match])
        base-url'        (base-url @app-db uri)
        url              (cond-> base-url'
                           params
                           (str "?" (h/to-query-params params)))]
    (go
      (try
        (let [resp
              (<p! (js/fetch url (clj->js fetch-opts)))

              status
              (.-status resp)

              event
              (if (<= status 299)
                success
                error)]
          (when (or (= status 401) (>= status 500))
            (throw (ex-info "Http error" resp)))

          (if (:dont-parse opts)
            (let [output (<p! (cond
                                (= (:response-type opts) :array-buffer)
                                (.arrayBuffer resp)

                                (= (:response-type opts) :blob)
                                (.blob resp)

                                :else
                                (.text resp)))
                  dispatch-events [[:xhr/done {:request opts :status status}]
                                   [(:event event)
                                    (merge event {:request opts :data output})
                                    (:params event)]]]
              (->> dispatch-events
                   (map rf/dispatch)
                   (doall)))

            (let [data
                  (<! (get-response-body resp))

                  dispatch-events
                  (cond-> [[:xhr/done {:request opts :data data :status status}]]

                    event
                    (conj (if (vector? (:event event))
                            (:event event)
                            [(:event event)
                             (merge event {:request opts :data data :status status})
                             (:params event)]))

                    (and (> status 299) (not (:flash-disabled opts)))
                    (into [[:close-loading]
                           (error-flash-view pid base-url' url resp x-correlation-id data)]))]
              (->> dispatch-events
                   (map rf/dispatch)
                   (doall)))))
        (catch js/Error err
          (let [ex
                (or (ex-cause err) err)

                resp
                (ex-data err)

                status
                (.-status resp)

                data
                (<! (get-response-body resp))

                flash-danger
                (error-flash-view pid base-url' url resp x-correlation-id data)

                dispatch-events
                (cond
                  (>= status 500)
                  (cond-> []
                    (not (:flash-disabled opts))
                    (conj flash-danger)

                    error
                    (conj [(:event error) (merge error {:request opts :error ex :data data :status status}) (:params error)]))

                  (= status 401)
                  [[:flash/danger {:msg "Пользователь не авторизован"}]
                   [::auth/logout-done]]

                  (= (.-name ex) "TypeError")
                  [[:flash/danger {:msg "Отсутствует интернет соединение"}]]

                  (not= (.-name ex) "AbortError")
                  (cond-> [[:xhr/done {:request opts :data data :status status}]]

                    (not (:flash-disabled opts))
                    (conj flash-danger)

                    (:event error)
                    (conj [(:event error) {:request opts :error resp :data data :status status} (:params error)])))]
            (->> (conj dispatch-events [:close-loading])
                 (map rf/dispatch)
                 (doall))))
        (catch :default err (prn "Unexpected error" err))))))

(rf/reg-event-fx
 ::report-sended
 (fn [{db :db} _]
   {:db (dissoc db :flash)
    :dispatch [:flash/info {:msg [:div [:div "Сообщение об ошибке отправлено."] [:div "Спасибо за вашу помощь."]]}]}))

(rf/reg-event-fx
 :xhr/error-report
 (fn [{db :db} [_ {:keys [msg] :as opts}]]
   {:xhr/fetch {:uri "/$error-report"
                :method :post
                :body {:msg msg}
                :success {:event ::report-sended}}}))

(defn json-fetch!
  [opts role]
  (if (vector? opts)
    (doseq [o opts] (*json-fetch o role))
    (*json-fetch opts role)))

(defn- xhr-fetch!
  [opts]
  (let [opts       (if (map? opts) [opts] (vec opts))
        clean-opts (filterv identity opts)
        role       (get-in @app-db [:zframes.auth/userinfo :current-role])
        db         (reduce (fn [acc {:keys [paging req-id] {:keys [_page]} :params}]
                             (let [current-page (get-in acc [:xhr :req req-id :page])]
                               (cond-> acc
                                 req-id
                                 (assoc-in [:xhr :req req-id :loading] true) ; Set loading

                                 (> _page 1)
                                 (assoc-in [:xhr :req req-id :s-loading] true) ; Set scroll loading

                                 (and (> current-page (or _page 1)) paging)    ; Remove old response
                                 (dissoc-in [:xhr :req req-id]))))
                           @app-db
                           clean-opts)]
    (reset! app-db db)
    (json-fetch! opts role)))

(rf/reg-fx :xhr/fetch xhr-fetch!)
(rf/reg-fx :xhr/fetch-abort #(rf/dispatch [:xhr/fetch-abort %]))

(rf/reg-event-fx
 :xhr/fetch
 (fn [_ [_ opts]]
   {:xhr/fetch opts}))

(rf/reg-event-fx
 :xhr/paging
 (fn [{db :db} [_ req-id]]
   (let [{{{:keys [_page] :as params} :params :as req} :request :as resp} (get-in db [:xhr :req req-id])]
     (when resp
       {:dispatch [:xhr/fetch (assoc-in req [:params :_page] (if _page (inc _page) 2))]}))))

(rf/reg-event-fx
 :xhr/fetch-abort
 (fn [{db :db} [_ {:keys [req-id] :as opts}]]
   (when-let [ctrl (get @abort-controller-cache req-id)]
     (.abort ctrl))
   {:db (assoc-in db [:xhr :req req-id :loading] false)}))

(rf/reg-event-fx
 :xhr/done
 (fn [{db :db} [_ {:keys [request data status] {:keys [req-id paging] {:keys [_count _page]} :params} :request}]]
   (let [entry (or (:entry data)
                   (-> data :hits :hits))]
     {:db (if req-id
            (cond-> db
              true         (-> (assoc-in [:xhr :req req-id :request]           request)
                               (assoc-in [:xhr :req req-id :status]            status)
                               (assoc-in [:xhr :req req-id :loading]           false)
                               (assoc-in [:xhr :req req-id :paging]            paging))
              paging       (-> (assoc-in [:xhr :req req-id :show-more?]
                                         (and (= _count
                                                 (count entry))
                                              (seq entry)))
                               (assoc-in [:xhr :req req-id :page]              (or _page 1))
                               (assoc-in [:xhr :req req-id :data (or _page 1)] data))
              (not paging) (assoc-in [:xhr :req req-id :data]                  data)
              (> _page 1)  (assoc-in [:xhr :req req-id :s-loading]             false))
            db)})))

(rf/reg-event-fx
 :xhr/remove-response
 (fn [{db :db} [_ req-id success]]
   (cond->
    {:db (dissoc-in db [:xhr :req req-id])}

     success
     (assoc :dispatch success))))

(rf/reg-event-fx
 :xhr/update-response
 (fn [{db :db} [_ req-id {:keys [items-path id-key new-item]}]]
   {:db (update-in db (into [:xhr :req req-id :data] items-path)
                   (partial map #(if (= (get % id-key)
                                        (get new-item id-key))
                                   new-item
                                   %)))}))

(rf/reg-event-fx
 :xhr/deinit-page
 (fn [{db :db} [_ page success]]
   (let [page-namespace (some->> page namespace (str ":"))
         remove-ns      [":app.form.zenform-events"]]
     (merge {:db (sp/setval [:xhr :req sp/MAP-KEYS
                             (fn [k]
                               (and
                                k
                                (some #(str/starts-with? (str k) %) (conj remove-ns page-namespace))))]
                            sp/NONE
                            db)}
            (when success {:dispatch success})))))

(rf/reg-event-fx
 :xhr/deinit-everything
 (fn [{db :db} [_ page success]]
   (let [page-namespaces (if (sequential? page)
                           (map #(some->> % namespace (str ":")) page)
                           (some->> page namespace (str ":") (vector)))
         remove-ns       (into [":app.form.zenform-events"] page-namespaces)]
     (merge {:db (->> (dissoc db page :button-spinner :expand-block)
                      (sp/setval [sp/MAP-KEYS (fn [k]
                                                (and
                                                 k
                                                 (some #(str/starts-with? (str k) %) remove-ns)))]
                                 sp/NONE)
                      (sp/setval [:xhr :req sp/MAP-KEYS
                                  (fn [k]
                                    (and
                                     k
                                     (some #(str/starts-with? (str k) %) remove-ns)))]
                                 sp/NONE))}
            (when success {:dispatch success})))))

(rf/reg-sub
 :xhr/response
 (fn [db [_ req-id]]
   (let [{:keys [data paging] :as req} (get-in db [:xhr :req req-id])]
     (if paging
       (assoc-in req [:data :entry] (mapcat (comp :entry val) data))
       req))))

(rf/reg-sub
 :xhr/loading
 (fn [db [_ req-id]]
   (let [{:keys [data paging] :as req} (get-in db [:xhr :req req-id])]
     (if paging
       (assoc-in req [:data :entry] (mapcat (comp :entry val) data))
       (:loading req)))))

(rf/reg-cofx
 :xhr/response
 (fn [{:keys [db] :as cofx} req-id]
   (let [{:keys [data paging] :as req}
         (get-in db [:xhr :req req-id])

         req' (if paging
                (assoc-in req [:data :entry] (mapcat (comp :entry val) data))
                req)]
     (assoc-in cofx [:xhr/response req-id] req'))))

(rf/reg-sub
 :xhr/resources
 (fn [[_ req-id _] _]
   [(rf/subscribe [:xhr/response req-id])])
 (fn [[response] _]
   (cu/resources response)))

(rf/reg-sub
 :xhr/resource
 (fn [[_ req-id _] _]
   [(rf/subscribe [:xhr/response req-id])])
 (fn [[response] _]
   (cu/data->resource response)))

(rf/reg-event-fx
 :xhr/redirect
 (fn [_ [_ _ opts]]
   {:dispatch [:zframes.redirect/redirect opts]}))

(rf/reg-sub
 :xhr/config
 (fn [db _]
   (get-in db [:xhr :config])))
