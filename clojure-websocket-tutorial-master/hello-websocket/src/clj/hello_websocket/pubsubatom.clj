(ns hello-websocket.pubsubatom
  (:use org.httpkit.server))

(def my-channel (atom nil))

(defn handler [request]
  (with-channel request channel
    (on-close channel (fn [status]        
                            (remove-watch my-channel channel)))
    (on-receive channel (fn [json]
                              (reset! my-channel json)))
    (add-watch my-channel channel
      (fn [_channel _atom _old_json json]
        (send! channel json)))))

