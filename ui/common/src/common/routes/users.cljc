(ns common.routes.users)

(def search :app.users.controller/index)
(def common :app.users.controller/common)
(def create :app.users.controller/common)
(def edit   :app.users.controller/common)

(def routes
  {"users"  {:.       search
             :title   "Список пользователей"
             "create" {:.     create
                       :title "Добавление нового пользователя"}
             [:id]    {:.     edit
                       :title "Редактирование пользователя"}}})