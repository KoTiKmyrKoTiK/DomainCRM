(ns common.routes.login)

(def root  :app.login.controller/login-index)
(def login :app.login.controller/login)

(def routes
  {"login"  {:.       login
             :navbar? false
             :title   "Авторизация"}})