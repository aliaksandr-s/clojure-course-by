(ns datomic
  (:require
    [datomic.api :as d]
    [clojure.edn :as edn]
    [clojure.java.io :as io]))

(def db-url "datomic:free://localhost:4334/imdb")

(d/create-database db-url)
(def conn (d/connect db-url))
(defn db [] (d/db conn))


;; hint: сделайте extenal id из feature :id
(def schema
  [{:db/index       true
    :db/ident       :feature/type
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :series}

   {:db/ident :episode}

   {:db/ident :movie}

   {:db/ident :video}

   {:db/ident :tv-movie}

   {:db/ident :videogame}

   {:db/index       true
    :db/ident       :feature/id
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   {:db/index       true
    :db/ident       :feature/title
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :feature/year
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :feature/endyear
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :feature/series
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :feature/season
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :feature/episode
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}])


(defn reset []
  (d/release conn)
  (d/delete-database db-url)
  (d/create-database db-url)
  (alter-var-root #'conn (constantly (d/connect db-url)))
  @(d/transact conn schema))

(reset)


;; Формат файла:
;; { :type  =>   :series | :episode | :movie | :video | :tv-movie | :videogame
;;   :id    =>   str,  unique feature id
;;   :title =>   str,  feature title. Optional for episodes
;;   :year  =>   long, year of release. For series, this is starting year of the series
;;   :endyear => long, only for series, optional. Ending year of the series. If omitted, series still airs
;;   :series  => str,  only for episode. Id of enclosing series
;;   :season  => long, only for episode, optional. Season number
;;   :episode => long, only for episode, optional. Episode number
;; }
;; hint: воспользуйтесь lookup refs чтобы ссылаться на features по внешнему :id

(defn remove-nil 
  [map]
  (into {} (remove (comp nil? second) map)))


(defn import []
  (with-open [rdr (io/reader "features.2014.edn")]
    (doseq [line (line-seq rdr)
            :let [feature (edn/read-string line)
                  feature-map {:db/id           (d/tempid :db.part/user)
                               :feature/type    (:type feature)
                               :feature/id      (:id feature)
                               :feature/title   (:title feature)
                               :feature/year    (:year feature)
                               :feature/endyear (:endyear feature)
                               :feature/series  (when-let [series (:series feature)] [:feature/id series])
                               :feature/season  (:season feature)
                               :feature/episode (:episode feature)
                               }
                  entity (remove-nil feature-map)]]
      @(d/transact conn [entity]))))

(import)


;; Найти все пары entity указанных типов с совпадающими названиями
;; Например, фильм + игра с одинаковым title
;; Вернуть #{[id1 id2], ...}
;; hint: data patterns


(defn siblings [db type1 type2]
  (d/q '[:find ?id1 ?id2
         :in $ ?type1 ?type2
         :where
         [?t-id1 :feature/type ?type1]
         [?t-id2 :feature/type ?type2]
         [?t-id1 :feature/title ?title]
         [?t-id2 :feature/title ?title]
         [?t-id1 :feature/id ?id1]
         [?t-id2 :feature/id ?id2]] 
       db type1 type2))

(siblings (db) :movie :videogame)


;; Найти сериал(ы) с самым ранним годом начала
;; Вернуть #{[id year], ...}
;; hint: aggregates

(defn- min-year [db]
  (d/q '[:find (min ?year) .
         :where
         [?id :feature/year ?year]]
       db))

(defn oldest-series [db]
  (let [m-year (min-year db)]
    (d/q '[:find ?title ?year
           :in $ ?year
           :where
           [?id :feature/year ?year]
           [?id :feature/id ?title]
           [?id :feature/type :series]]
         db m-year)))

(oldest-series (db))


;; Найти 3 сериала с наибольшим количеством серий в сезоне
;; Вернуть [[id season series-count], ...]
;; hint: aggregates, grouping

(defn longest-season-1 [db]
  (->> (d/q '[:find ?s-title ?e-season ?e-id
              :where
              [?e-id :feature/type :episode]
              [?e-id :feature/season ?e-season]
              [?e-id :feature/series ?s-id]
              [?s-id :feature/id ?s-title]]
            db)
       (sort #(compare (first %1) (first %2)))
       (group-by (juxt first second))
       (vec)
       (map #(conj (first %) (count (second %))))
       (sort-by #(nth % 2))
       (reverse)
       (take 3)
       (vec)))

(longest-season-1 (db))


(defn longest-season [db]
  (->> (d/q '[:find ?title ?season (count ?s-id)
              :where
              [?s-id :feature/season ?season]
              [?s-id :feature/series ?series-id]
              [?series-id :feature/id ?title]]
            db) 
       (sort-by #(nth % 2) >)
       (take 3)
       (vec)))

(longest-season (db))

;; Найти 5 самых популярных названий (:title). Названия эпизодов не учитываются
;; Вернуть [[count title], ...]
;; hint: aggregation, grouping, predicates

(defn popular-titles [db]
  (->> (d/q '[:find (count ?f-id) ?title
              :where
              [?f-id :feature/title ?title]
              [?f-id :feature/type ?type]
              [?e-id :db/ident :episode]
              [(not= ?type ?e-id)]] db)
       (sort-by first >)
       (take 5)
       (vec)))

(popular-titles (db))