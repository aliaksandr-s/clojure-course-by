(ns ring-demo.core
  (:require [compojure.route :as route])
  (:use ring.adapter.jetty
        ring.middleware.content-type
        ring.middleware.session
        ring.middleware.file
        ring.middleware.resource
        ring.middleware.file-info
        [ring.middleware.json :only [wrap-json-response wrap-json-body]]
        ring.util.response
        compojure.core))


;; 1) Hello world!
;;
(comment
  (defn handler [request]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body "Hello, World!"})
  )

;; Способы запуска:
;; a) lein ring server (:ring {:handler ring-demo.core/handler})
;; b) (defonce server (run-jetty app {:port 8080 :join? false}))
;;    :join -- false -- daemon, true -- wait for return

;; 2) Для экспериментов лучше использовать lein ring server, т.к. он
;;    перезагружает код каждый раз, когда он меняется
;;
;; project.clj:
;; :ring {:handler ring-demo.core/app}

(comment
  (defn handler [request]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body "Hello, World!"})

  (def app handler)
  )


;; 3) Показать, что такое request:
;;
(comment
  (defn handler [request]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (str request)})

  (def app handler)
  )

;; 4) Упрощенный ответ
;;
(comment
  (defn handler [request]
    (-> (response "The response")
        (content-type "text/plain")))

  (def app handler)
  )



;; 5) Написать свой простой враппер
;;
(comment
  (defn handler [request]
    (response (str request)))


  (defn wrap-my-response [handler]
    (fn [request]
      (let [response (handler request)]
        (assoc response :headers
               (assoc (:headers request)
                 "Content-Type" "text/plain")))))

  (def app
    (wrap-my-response handler))
  )

;; 6) Воспользоваться готовым враппером
;; Показывать только в Poster-e
;; Список расширений файлов и их mime-type:
;; https://github.com/mmcgrana/ring/blob/master/ring-core/src/ring/util/mime_type.clj
;;
(comment
  (defn handler [request]
    (response (str request)))

  (def app
    (-> handler
        wrap-content-type))
  )


;; 7) Добавить cвой mime-type для расширения файла
;; Показывать только в Poster-e
;;
(comment
  (defn handler [request]
    (response (str request)))

  (def app
    (-> handler
        (wrap-content-type {:mime-types {"foo" "text/x-foo"}})))
  )

;; 8) Враппер для сессии
;; Показывать только в Browser-e. Показать сессию в запросе
;; Объяснить как работает враппер сессии
;; https://github.com/ring-clojure/ring/blob/master/ring-core/src/ring/middleware/session.clj
;;
(comment
  (defn handler [request]
    (-> (response (str request))
        (assoc :session {:abc 123})))

  (def app
    (-> handler
        wrap-session))
  )

;; 9) Еще пример сессии
;;
(comment
  (defn handler [request]
    (let [cnt (or (:my-count (:session request)) 0)]
      (-> (response (str "You've accessed " cnt " times"))
          (assoc :session {:my-count (inc cnt)}))))

  (def app
    (-> handler
        wrap-session))
  )


;; 10) (wrap-file "/var/www") (wrap-resource "public")
;; https://github.com/mmcgrana/ring/wiki/Static-Resources
(comment
  (defn handler [request]
    (response (str request)))

  (def app
    (-> handler
        wrap-session
        (wrap-file "/var/www")))
  )

(comment
  (defn handler [request]
    (response (str request)))

  (def app
    (-> handler
        wrap-session
        (wrap-file "/var/www")
        (wrap-resource "public")
        (wrap-file-info))) ;; checks the modification dates and the file extension of the file, adding Content-Type and Last-Modified headers. This makes sure the browser knows the type of the file being served, and doesn't re-request the file if its already in the browser's cache.
  )

;; 11) Restrict access
;; Ограничение доступа к файлу
(comment
  (defn handler [request]
    (response (str request)))

  (defn wrap-restrict [handler regexp]
    (fn [request]
      (let [uri (:uri request)]
        (if (re-matches regexp uri)
          (response "Access to this page is restricted!")
          (handler request)))))

  (def app
    (-> handler
        ;; (wrap-restrict #"/index1.html") -- Если оставить здесь, то отдаст html
        wrap-session
        (wrap-file "/var/www")
        (wrap-resource "public")
        (wrap-file-info)
        (wrap-restrict #"/index1.html")))
  )


(comment ;; Ограничение доступа к URL
  (defn handler [request]
    (let [cnt (or (:my-count (:session request)) 0)]
      (-> (response (str "You've accessed " cnt " times"))
          (assoc :session {:my-count (inc cnt)}))))

  (defn wrap-restrict [handler regexp]
    (fn [request]
      (let [uri (:uri request)]
        (if (re-matches regexp uri)
          (redirect "/denied.html")
          (handler request)))))

  (def app
    (-> handler
        wrap-session
        (wrap-file "/var/www")
        (wrap-resource "public")
        (wrap-file-info)
        (wrap-restrict #"/aaa")))
  )

;; 12) JSON
;; https://github.com/ring-clojure/ring-json
;;
;; wrap-json-response
(comment
  (defn handler [request]
    (response {:hello "World!"}))

  (def app
    (wrap-json-response handler))
  )

;; wrap-json-body
;;
;; Для теста отправить POST-запрос с типом application/json
;; и телом {"my": "param"}
;;
(comment
  (defn handler [request]
    (response (str (:body request))))

  (def app
    (wrap-json-body handler {:keywords? true :bigdecimals? true}))
  )

;; The wrap-json-params middleware will parse any request with a JSON
;; content-type and body and merge the resulting parameters into a params map






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Compojure

;; 1) Замена Ring на Compojure
;;
(:require [compojure.route :as route])
(:use compojure.core)


(defroutes compojure-handler
  (GET "/" request (str request))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

(defn wrap-restrict [handler regexp]
  (fn [request]
    (let [uri (:uri request)]
      (if (re-matches regexp uri)
        (redirect "/denied.html")
        (handler request)))))

(def app
  (-> compojure-handler
      wrap-session
      ;; (wrap-file "/var/www")
      ;; (wrap-resource "public")
      ;; (wrap-file-info)
      (wrap-restrict #"/index1.html")
      wrap-json-response
      (wrap-json-body {:keywords? true :bigdecimals? true})
      ))


;; 2) Redirect-ы
;;
(defroutes compojure-handler
  (GET "/" request (str request))
  (GET "/abc" request (redirect "/denied.html"))
  (route/resources "/")
  ;; (route/not-found (redirect "/denied.html")) -- не работает
  (route/not-found (slurp (io/resource "public/denied.html"))))
  

(defn wrap-restrict [handler regexp]
  (fn [request]
    (let [uri (:uri request)]
      (if (re-matches regexp uri)
        (redirect "/denied.html")
        (handler request)))))

(def app
  (-> compojure-handler
      wrap-session
      (wrap-restrict #"/index1.html")
      wrap-json-response
      (wrap-json-body {:keywords? true :bigdecimals? true})
      ))

;; 3) compojure.handler/site
;; http://weavejester.github.io/compojure/compojure.handler.html
;; (:use compojure.handler)
;;
(defroutes compojure-handler
  (GET "/" request (str request))
  (GET "/abc" request (redirect "/denied.html"))
  (route/resources "/")
  (route/not-found (slurp (io/resource "public/denied.html"))))

(def app
  (-> compojure-handler
      site))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 3) Destructuring
;; 
(defn main-page [request]
  (let [cnt (or (:my-count (:session request)) 0)]
      (-> (response (str "You've accessed " cnt " times"))
          (assoc :session {:my-count (inc cnt)}))))

(defroutes compojure-handler
  (GET "/" request (main-page request))
  (GET "/abc" request (redirect "/denied.html"))
  (route/resources "/")
  (route/not-found (slurp (io/resource "public/denied.html"))))

(def app
  (-> compojure-handler
      site))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defroutes compojure-handler
  (GET "/" request (main-page request))
  (GET "/req" request (str request)) ;; Показать, что появились :params
  (GET "/abc" request (redirect "/denied.html"))
  (route/resources "/")
  (route/not-found (slurp (io/resource "public/denied.html"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn main-page [{:keys [session]}]
  (let [cnt (or (:my-count session) 0)]
      (-> (response (str "You've accessed " cnt " times"))
          (assoc :session {:my-count (inc cnt)}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn main-page [{:keys [my-count]}]
  (-> (response (str "You've accessed " my-count " times"))
      (assoc :session {:my-count (inc (or my-count 0))})))

(defroutes compojure-handler
  (GET "/" {:keys [session]} (main-page session))
  (GET "/req" request (str request))
  (GET "/abc" request (redirect "/denied.html"))
  (route/resources "/")
  (route/not-found (slurp (io/resource "public/denied.html"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn main-page [my-count]
  (-> (response (str "You've accessed " my-count " times"))
      (assoc :session {:my-count (inc (or my-count 0))})))

(defroutes compojure-handler
  (GET "/" {{my-count :my-count} :session} (main-page my-count))
  (GET "/req" request (str request))
  (GET "/abc" request (redirect "/denied.html"))
  (route/resources "/")
  (route/not-found (slurp (io/resource "public/denied.html"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defroutes compojure-handler
  (GET "/" {:keys [session]} (main-page session))
  ;; assign whole request to req through :as
  (GET "/:a/:b/:c" [a b c :as req] (str a "-" b "-" c " and " req)) 
  (GET "/:a/:b" [a b] (str a "-" b))
  (GET "/req" request (str request))
  (GET "/abc" request (redirect "/denied.html"))
  (route/resources "/")
  (route/not-found (slurp (io/resource "public/denied.html"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 4) Показать context
;;

(defroutes my-routes
  (GET "/req" [] (str "Request from my-routes")))

(defroutes compojure-handler
  (GET "/" {:keys [session]} (main-page session))
  (GET "/req" request (str request))
  (GET "/abc" request (redirect "/denied.html"))
  (context "/my" [] my-routes)
  (route/resources "/")
  (route/not-found (slurp (io/resource "public/denied.html"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 5) File upload
;;
(defroutes my-routes
  (GET "/req" [] (str "Request from my-routes"))
  (GET "/form" [] "<FORM action='/my/form-upload'
       enctype='multipart/form-data'
       method='post'>
   Name: <INPUT type='text' name='submit-name'/><BR/>
   File: <INPUT type='file' name='myfile'/><BR/>
   <INPUT type='submit' value='Send'></FORM>")
  (POST "/form-upload" [submit-name myfile] (str submit-name " ### " myfile)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn upload [submit-name {:keys [tempfile filename]}]
  (let [out (io/file filename)]
    (io/copy tempfile out)
    (response (str submit-name ", file is submitted!"))))

(defroutes my-routes
  (GET "/req" [] (str "Request from my-routes"))
  (GET "/form" [] "<FORM action='/my/form-upload'
       enctype='multipart/form-data'
       method='post'>
   Name: <INPUT type='text' name='submit-name'/><BR/>
   File: <INPUT type='file' name='myfile'/><BR/>
   <INPUT type='submit' value='Send'></FORM>")
  (POST "/form-upload" [submit-name myfile] (upload submit-name myfile)))




