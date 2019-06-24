(ns async-demo.core
  (:use clojure.core.async))


;;; part-1

(def c (chan 10))

(put! c 42 (fn [v] (println "put!" v)))
(take! c (fn [v] (println "got" v)))

(>!! c 1)
(>!! c 2)
(>!! c 3)

(<!! c)

(close! c)

(def a (chan 10))
(def b (chan 10))
(put! a 33)


(alts!! [[a 18] b])
(<!! a)

(alts!! [a (timeout 1000)])


;;; part-2

(go
  (<! c)
  (println "waiting")
  (>! c 42)
  (alts! [a b]))

(defn req [server query]
  (<!! (timeout (rand-int 100)))
  (str query " from " server))

(defn fastest [query & servers]
  (let [c (chan)]
    (doseq [server servers]
      (go
        (>! c (req server query))
        (close! c)))
    c))

(defn search [query]
  (let [c (chan)
        t (timeout 80)]
    (go (>! c (<! (fastest query "web1" "web2" "web2"))))
    (go (>! c (<! (fastest query "img1" "img2"))))
    (go (>! c (<! (fastest query "video1" "video2"))))
    (go (loop [ret []]
          (if (>= (count ret) 3)
            ret
            (recur 
             (conj ret (alt! [c t] ([v] v)))))))))

(<!! (search "abc"))


;;; part 3

(let [c (chan 10)
      d (map< #(- %) c)]
  (>!! c 42)
  (<!! d))

(let [[ch1 ch2] (split even? ch)])

(let [in (chan 10)
      o1 (chan 10)
      o2 (chan 10)
      p (pub in :tag)]
  (sub p :cats o1)
  (sub p :dogs o2)
  (>!! in {:tag :dogs})
  (>!! in {:tag :cats})
  [(<!! o1) (<!! o2)])