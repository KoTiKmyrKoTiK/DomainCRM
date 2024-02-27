(ns common.routes.domains)

(def search :app.domains.controller/search)

(def common :app.domains.controller/common)

(def create :app.domains.controller/create)
(def edit   :app.domains.controller/edit)

(def request-replace :app.domains.controller/replace)
(def requests        :app.domains.controller/requests)

(def routes
  {"domains" {:.         search
              :title     "Домены"
              "create"   {:.     create
                          :title "Добавление нового домена"}
              "requests" {:.     requests
                          :title "Добавление нового домена"}
              [:id]      {:.        edit
                          :title    "Редактирование домена"
                          "replace" {:.     request-replace
                                     :title "Запрос на замену домена"}}}})