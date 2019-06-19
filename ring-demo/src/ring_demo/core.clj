(ns ring-demo.core
  (:require [compojure.route :as route]
            [clojure.java.io :as io])
  (:use ring.adapter.jetty
        ring.middleware.content-type
        ring.middleware.session
        ring.middleware.file
        ring.middleware.resource
        ring.middleware.file-info
        [ring.middleware.json :only [wrap-json-response wrap-json-body]]
        ring.util.response
        compojure.core
        compojure.handler))

(defn handler [request]
  (response (str request)))

(defn main-page [my-count]
  (-> (response (str "You've accessed " (or my-count 0) " times"))
      (assoc :session {:my-count (inc (or my-count 0))})))

(defroutes person-handler
  (GET "/req" [id] (str "Request from person-handler id: " id)))

(defn upload [submit-name {:keys [tempfile filename]}]
  (let [out (io/file filename)]
    (io/copy tempfile out)
    (response (str submit-name ", file is submitted!"))))

(defroutes form-routes
  (GET "/req" [] (str "Request from form-routes"))
  (GET "/form" [] "<FORM action='/form/form-upload'
       enctype='multipart/form-data'
       method='post'>
   Name: <INPUT type='text' name='submit-name'/><BR/>
   File: <INPUT type='file' name='myfile'/><BR/>
   <INPUT type='submit' value='Send'></FORM>")
  (POST "/form-upload" [submit-name myfile] (upload submit-name myfile)))

(defroutes compojure-handler
  (context "/person/:id" [] person-handler)
  (context "/form" [] form-routes)
  (GET "/" {{my-count :my-count} :session} (main-page my-count))
  (GET "/my/:a/:b" [a b :as req] (str "a=" a ";b=" b "     " req))
  (GET "/abc" request "my abc")
  (GET "/req" request (str request))
  (route/resources "/")
  (route/not-found (slurp (io/resource "public/denied.html"))))

(def app 
  (site compojure-handler))