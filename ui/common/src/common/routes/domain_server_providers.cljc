(ns common.routes.domain-server-providers)

(def search :app.domain-server-providers.controller/index)
(def create :app.domain-server-providers.controller/create)
(def common :app.domain-server-providers.controller/common)
(def edit   :app.domain-server-providers.controller/edit)

(def routes
  {"domain_server_providers"  {:.       search
                               :title   "Список провайдеров серверов"
                               "create" {:.     create
                                         :title "Добавление нового провайдера сервера"}
                               [:id]    {:.     edit
                                         :title "Редактирование провайдера сервера"}}})