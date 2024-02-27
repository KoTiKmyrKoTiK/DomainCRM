(ns common.routes)

(def pid-login       :app.login.controller/login)
(def pid-login-index :app.login.controller/login-index)

(def pid-dashboard   :app.home.controller/dashboard)

(def pid-users        :app.users.controller/index)
(def pid-users-common :app.users.controller/common)
(def pid-users-create :app.users.crud.controller/create)
(def pid-users-edit   :app.users.crud.controller/edit)

(def routes
  {:.     pid-login-index
   "login"  {:.       pid-login
             :navbar? false
             :title   "Авторизация"}
   "logout" {:. :zframes.auth/logout}
   "home"   {:.     pid-dashboard
             :title "Dashboard"}
   "users"  {:.       pid-users
             :title   "Список пользователей"
             "create" {:.     pid-users-create
                       :title "Добавление нового пользователя"}
             [:id]    {:.     pid-users-edit
                       :title "Редактирование пользователя"}}})

(defn route-index*
  [route pth]
  (merge
   (hash-map (str (:. route))
             (assoc route :pth pth))
   (reduce-kv
    (fn [acc k v]
      (if (or (string? k) (vector? k))
        (merge acc (route-index* v (into pth [k "/"])))
        acc))
    {}
    route)))

(def route-index
  (route-index* routes ["/"]))
