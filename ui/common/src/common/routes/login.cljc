(ns common.routes.login)

(def login :app.login.controller/login)

(def routes
  {"login"  {:.       login
             :navbar? false
             :title   "Авторизация"}})