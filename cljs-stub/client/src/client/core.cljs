(ns client.core
  (:require [enfocus.core :as ef])
  (:require-macros [enfocus.macros :as em]))

(em/defsnippet blog-header "/html/blog.html" ".blog-header" [])

(defn start []
  (ef/at "body" (ef/content "hi")))

;; (set! (.-onload js/window) start)
(set! (.-onload js/window) #(em/wait-for-load (start)))