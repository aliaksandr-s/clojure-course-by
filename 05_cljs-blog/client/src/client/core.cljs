(ns client.core
  (:require [enfocus.core :as ef]
            [ajax.core :refer [GET POST]])
  (:require-macros [enfocus.macros :as em]))

(em/defsnippet blog-header "/html/blog.html" ".blog-header" [])
(em/defsnippet blog-sidebar "/html/blog.html" "#sidebar" [])
(em/defsnippet blog-content "/html/blog.html" "#content" [])
(em/defsnippet blog-create-post "/html/blog.html" "#article-form" [])

(em/defsnippet blog-edit-post "/html/blog.html" "#article-form"
  [{:keys [id title body]}]
  "#article-title" (ef/set-attr :value title)
  "#article-body" (ef/content body)
  "#save-btn" (ef/set-attr :onclick (str "client.core.try_update_article(" id ")")))

(em/defsnippet blog-post-view "/html/blog.html" "#view-article" [{:keys [title body]}]
  "#view-title" (ef/content title)
  "#view-body" (ef/content body))

(em/defsnippet blog-post "/html/blog.html"
  ".blog-post"  [{:keys [id title body]}]
  "#blog-post-title" (ef/content title)
  "#blog-post-body" (ef/content body)
  "#article-view" (ef/set-attr :onclick
                    (str "client.core.try_view_article(" id ")"))
  "#article-edit" (ef/set-attr :onclick
                    (str "client.core.try_edit_article(" id ")"))
  "#article-delete" (ef/set-attr :onclick
  (str "if(confirm('Really delete?')) client.core.try_delete_article(" id ")")))

(defn article-list [data]
  (ef/at "#inner-content" (ef/content (map blog-post data))))

(defn try-load-articles []
  (GET "/article/list"
       {:handler article-list}))

(defn start []
  (ef/at ".container"
         (ef/do-> (ef/content (blog-header))
                  (ef/append (blog-content))
                  (ef/append (blog-sidebar))))
  (try-load-articles))

(defn hide-new-post-btn []
  (ef/at "#new-post" (ef/set-attr :style "display:none;")))

(defn ^:export show-create []
  (ef/at "#inner-content" (ef/content (blog-create-post)))
  (hide-new-post-btn))

(defn ^:export close-form []
  (start))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "Something bad happened: " status " " status-text)))

(defn article-saved [response]
  (close-form))

(defn ^:export try-update-article [id]
  (POST (str "/article/update/" id)
        {:params {:title (ef/from "#article-title" (ef/read-form-input))
                  :body  (ef/from "#article-body" (ef/read-form-input))}
         :handler article-saved
         :error-handler error-handler}))

(defn ^:export article-edit [data]
  (ef/at "#inner-content" (ef/content (blog-edit-post data)))
  (hide-new-post-btn))

(defn ^:export try-edit-article [id]
  (GET (str "/article/" id)
        {:handler article-edit
         :error-handler error-handler}))

(defn article-view [data]
  (hide-new-post-btn)
  (ef/at "#inner-content" (ef/content (blog-post-view data))))

(defn ^:export try-view-article [id]
  (GET (str "/article/" id)
        {:handler article-view
         :error-handler error-handler}))

(defn ^:export try-delete-article [id]
  (POST (str "/article/delete/" id)
        {:handler article-saved
         :error-handler error-handler}))

(defn ^:export try-create-article []
  (.log js/console (ef/from "#article-title" (ef/read-form-input)))
  (.log js/console (ef/from "#article-body" (ef/read-form-input)))
  (POST "/article/create"
        {:params {:title (ef/from "#article-title" (ef/read-form-input))
                  :body (ef/from "#article-body" (ef/read-form-input))}
         :handler article-saved
         :error-handler error-handler}))

;; (set! (.-onload js/window) start)
(set! (.-onload js/window) #(em/wait-for-load (start)))

