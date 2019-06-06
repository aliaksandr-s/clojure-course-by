(ns task02.query
  (:use [task02 helpers db])
  (:require [clojure.core.match :refer [match]]))

;; Функция выполняющая парсинг запроса переданного пользователем
;;
;; Синтаксис запроса:
;; SELECT table_name [WHERE column comp-op value] [ORDER BY column] [LIMIT N] [JOIN other_table ON left_column = right_column]
;;
;; - Имена колонок указываются в запросе как обычные имена, а не в виде keywords. В
;;   результате необходимо преобразовать в keywords
;; - Поддерживаемые операторы WHERE: =, !=, <, >, <=, >=
;; - Имя таблицы в JOIN указывается в виде строки - оно будет передано функции get-table для получения объекта
;; - Значение value может быть либо числом, либо строкой в одинарных кавычках ('test').
;;   Необходимо вернуть данные соответствующего типа, удалив одинарные кавычки для строки.
;;
;; - Ключевые слова --> case-insensitive
;;
;; Функция должна вернуть последовательность со следующей структурой:
;;  - имя таблицы в виде строки
;;  - остальные параметры которые будут переданы select
;;
;; Если запрос нельзя распарсить, то вернуть nil

;; Примеры вызова:
;; > (parse-select "select student")
;; ("student")
;; > (parse-select "select student where id = 10")
;; ("student" :where #<function>)
;; > (parse-select "select student where id = 10 limit 2")
;; ("student" :where #<function> :limit 2)
;; > (parse-select "select student where id = 10 order by id limit 2")
;; ("student" :where #<function> :order-by :id :limit 2)
;; > (parse-select "select student where id = 10 order by id limit 2 join subject on id = sid")
;; ("student" :where #<function> :order-by :id :limit 2 :joins [[:id "subject" :sid]])
;; > (parse-select "werfwefw")
;; nil

;; Синтаксис запроса:
;; SELECT table_name [WHERE column comp-op value] [ORDER BY column] [LIMIT N] [JOIN other_table ON left_column = right_column]

(defn make-where-function [col op val]
  (let [col-key (keyword col)
        op (-> op symbol resolve)]
    #(op (str (col-key %)) val)))

; ((make-where-function "id" "=" "10") {:id 10})

(defn generate-or-seq [key]
  (let [transform-funcs [#(.toUpperCase %) clojure.string/capitalize identity]] 
    (-> (map (fn [tr-fun] (tr-fun key)) transform-funcs)
        (conj :or))))

(def or-select (generate-or-seq "select"))
(def or-where (generate-or-seq "where"))
(def or-order (generate-or-seq "order"))
(def or-by (generate-or-seq "by"))
(def or-limit (generate-or-seq "limit"))
(def or-join (generate-or-seq "join"))
(def or-on (generate-or-seq "on"))

(defn parse-select [^String sel-string]
  (let [req-vec (-> sel-string
                    (.split " ")
                    (vec))]
    (match req-vec
      [or-select t_name or-where w_col comp-op val or-order or-by ord_by or-limit lim or-join o_table or-on l_col "=" r_col]
      (list t_name :where (make-where-function w_col comp-op val) :order-by (keyword ord_by) :limit (parse-int lim) :joins [[(keyword l_col) o_table (keyword r_col)]])
      [or-select t_name or-where w_col comp-op val or-order or-by ord_by or-limit lim]
      (list t_name :where (make-where-function w_col comp-op val) :order-by (keyword ord_by) :limit (parse-int lim))
      [or-select t_name or-where w_col comp-op val or-limit lim]
      (list t_name :where (make-where-function w_col comp-op val) :limit (parse-int lim))
      [or-select t_name or-where w_col comp-op val] 
      (list t_name :where (make-where-function w_col comp-op val))
      [or-select t_name] 
      (list t_name)
      :else nil)))

;; Выполняет запрос переданный в строке.  Бросает исключение если не удалось распарсить запрос

;; Примеры вызова:
;; > (perform-query "select student")
;; ({:id 1, :year 1998, :surname "Ivanov"} {:id 2, :year 1997, :surname "Petrov"} {:id 3, :year 1996, :surname "Sidorov"})
;; > (perform-query "select student order by year")
;; ({:id 3, :year 1996, :surname "Sidorov"} {:id 2, :year 1997, :surname "Petrov"} {:id 1, :year 1998, :surname "Ivanov"})
;; > (perform-query "select student where id > 1")
;; ({:id 2, :year 1997, :surname "Petrov"} {:id 3, :year 1996, :surname "Sidorov"})
;; > (perform-query "not valid")
;; exception...
(defn perform-query [^String sel-string]
  (if-let [query (parse-select sel-string)]
    (apply select (get-table (first query)) (rest query))
    (throw (IllegalArgumentException. (str "Can't parse query: " sel-string)))))
