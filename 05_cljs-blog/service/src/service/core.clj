(ns service.core
  (:require [compojure.route :as route]
            [clojure.java.io :as io])
  (:use compojure.core
        compojure.handler
        ring.middleware.edn
        carica.core
        korma.db
        korma.core))

(defdb db {:classname "com.mysql.jdbc.Driver"
           :subprotocol "mysql"
           :user (config :db :user)
           :password (config :db :pass)
           :subname (str "//127.0.0.1:3306/" (config :db :name) "?useUnicode=true&characterEncoding=utf8")
           :delimiters "`"})

(defentity article)

(defn response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn article-create [title body]
  (if-not (insert article (values {:title title :body body}))
    (response nil 420)
    (response nil)))

(defn article-list []
  (response (select article (order :id :DESC))))

(defn article-view [id]
  (response (first (select article (where {:id id})))))

(defn article-udpate [id title body]
  (response (update article
                    (set-fields {:title title
                                 :body body})
                    (where {:id id}))))

(defn article-delete [id]
  (response (delete article
                    (where {:id id}))))

(defroutes article-routes
  (POST "/delete/:id" [id] (article-delete id))
  (POST "/update/:id" [id title body] (article-udpate id title body))
  (GET "/list" [] (article-list))
  (POST "/create" [title body] (article-create title body))
  (GET "/:id" [id] (article-view id)))


(defroutes compojure-handler
  (GET "/" [] (slurp (io/resource "public/html/index.html")))
  (context "/article" [] article-routes)
  (GET "/req" request (str request))
  (route/resources "/")
  (route/files "/" {:root (config :external-resources)})
  (route/not-found "Not found!"))

(def app
  (-> compojure-handler
      site
      wrap-edn-params))
