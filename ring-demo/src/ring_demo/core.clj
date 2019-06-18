(ns ring-demo.core
  (:use ring.adapter.jetty
        ring.middleware.content-type
        ring.middleware.session
        ring.middleware.file
        ring.middleware.resource
        ring.middleware.file-info
        [ring.middleware.json :only [wrap-json-response wrap-json-body]]
        ring.util.response))

(defn handler [request]
  (response (str request)))

; (defn handler [request]
;   (let [cnt (or (:my-count (:session request)) 0)]
;     (-> (response {:times cnt})
;         (assoc :session {:my-count (inc cnt)}))))

(defn wrap-restrict [handler regexp]
  (fn [request]
    (let [uri (:uri request)]
      (if (re-matches regexp uri)
        (redirect "/denied.html")
        (handler request)))))

(def app
  (-> handler
      wrap-session
      (wrap-file "/home/alex/Desktop/")
      (wrap-resource "public")
      (wrap-file-info)
      (wrap-restrict #"/admin")
      (wrap-json-response)
      (wrap-json-body {:keywords? true :bigdecimals? true})))