(ns common.routes.users)

(def search :app.users.controller/index)
(def common :app.users.controller/common)
(def create :app.users.controller/create)
(def edit   :app.users.controller/edit)

(def routes
  {"users"  {:.       search
             :title   "Список пользователей"
             "create" {:.     create
                       :title "Добавление нового пользователя"}
             [:id]    {:.     edit
                       :title "Редактирование пользователя"}}})