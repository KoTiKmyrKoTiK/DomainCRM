(ns common.routes.domain-providers)

(def search :app.domain-providers.controller/index)
(def create :app.domain-providers.controller/create)
(def common :app.domain-providers.controller/common)
(def edit   :app.domain-providers.controller/edit)

(def routes
  {"domain_providers"  {:.       search
                        :title   "Список провайдеров доменов"
                        "create" {:.     create
                                  :title "Добавление нового провайдера домена"}
                        [:id]    {:.     edit
                                  :title "Редактирование провайдера домена"}}})