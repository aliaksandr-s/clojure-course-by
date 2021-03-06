(ns dsl.core
  (:use clojure.walk)
  (:import [java.util Calendar]))

(def cal (java.util.Calendar/getInstance))
(def today (java.util.Date.))
(def yesterday (do (.add cal java.util.Calendar/DATE -1) (.getTime cal)))
(def tomorrow (do (.add cal java.util.Calendar/DATE 2) (.getTime cal)))

(comment
  (defn one [] 1)
  
  ;; Примеры вызова
  (with-datetime
    (if (> today tomorrow) (println "Time goes wrong"))
    (if (<= yesterday today) (println "Correct"))
    (let [six (+ 1 2 3)
          d1 (today - 2 days)
          d2 (today + 1 week)
          d3 (today + six months)
          d4 (today + (one) year)]
      (if (and (< d1 d2)
               (< d2 d3)
               (< d3 d4))
        (println "DSL works correctly")))))


;; Поддерживаемые операции:
;; > >= < <=
;; Функция принимает на вход три аргумента. Она должна определить,
;; являются ли второй и третий аргумент датами. Если являются,
;; то из дат необходимо взять date.getTime и сравнить их по этому числу.
;; Если получены не даты, то выполнить операцию op в обычном порядке:
;; (op d1 d2).
(defn date? [x] (instance? java.util.Date x))

(defn d-op [op d1 d2]
  (if (and (date? d1) (date? d2)) 
    ((eval op) (.getTime d1) (.getTime d2))
    ((eval op) d1 d2)))

;; Пример вызова:
;; (d-add today '+ 1 'day)
;; Функция должна на основе своих параметров создать новую дату.
;; Дата создается при помощи календаря, например так:
;; (def cal (java.util.Calendar/getInstance))
;; (.add cal java.util.Calendar/DATE 2)
;; (.getTime cal)
;; Во-первых, необходимо на основе 'op' и 'num' определить количество, на
;; которое будем изменять дату. 'Op' может принимать + и -, соответственно
;; нужно будет не изменять либо изменить знак числа 'num'.
;; Во-вторых, необходимо узнать период, на который будем изменять дату.
;; Например, если получили 'day, то в вызове (.add cal ...) будем использовать
;; java.util.Calendar/DATE. Если получили 'months, то java.util.Calendar/MONTH.
;; И так далее.
;; Результат работы функции - новая дата, получаемая из календаря так: (.getTime cal)

(def metric-map {'day Calendar/DATE
                 'days Calendar/DATE
                 'week Calendar/WEEK_OF_YEAR
                 'weeks Calendar/WEEK_OF_YEAR
                 'month Calendar/MONTH
                 'months Calendar/MONTH
                 'year Calendar/YEAR
                 'years Calendar/YEAR
                 'hour Calendar/HOUR_OF_DAY
                 'hours Calendar/HOUR_OF_DAY
                 'minute Calendar/MINUTE
                 'minutes Calendar/MINUTE
                 'second Calendar/SECOND
                 'seconds Calendar/SECOND})

(defn d-add [date op num period]
  (let [cal (java.util.Calendar/getInstance)
        cal-method (metric-map period)
        num-comp (if (= op '-)
                   (- num)
                   num)]
    (do (.setTime cal date)
        (.add cal cal-method num-comp))
    (.getTime cal)))

;; Можете использовать эту функцию для того, чтобы определить,
;; является ли список из 4-х элементов тем самым списком, который создает новую дату,
;; и который нужно обработать функцией d-add.
(defn is-date-op? [code]
  (let [op (second code)
        period (last code)]
    (and (= (count code) 4)
         (or (= '+ op)
             (= '- op))
         (contains? #{'day 'days 'week 'weeks 'month 'months 'year 'years
                      'hour 'hours 'minute 'minutes 'second 'seconds} period ))))

(defn is-date-comp? [code]
  (let [op (first code)]
    (and (= (count code) 3)
         (contains? #{'> '>= '< '<=} op))))

;; В code содержится код-как-данные. Т.е. сам code -- коллекция, но его содержимое --
;; нормальный код на языке Clojure.
;; Нам необходимо пройтись по каждому элементу этого кода, найти все списки из 3-х элементов,
;; в которых выполняется сравнение, и подставить вместо этого кода вызов d-op;
;; а для списков из четырех элементов, в которых создаются даты, подставить функцию d-add.

(defn- add-dates-replacer [ls]
  (let [date (first ls)
        op (second ls)
        num (nth ls 2)
        period (last ls)]
    `(d-add ~date '~op ~num '~period)))


(defmacro with-datetime [& code]
  (println "code" code)
  `(do ~@(postwalk (fn [ls] 
                (if (seq? ls) 
                  (cond
                    (is-date-op? ls) (add-dates-replacer ls)
                    (is-date-comp? ls) (conj ls 'd-op)
                    :else ls)
                    ; :else (do (println "from cond" ls) (with-datetime ls)))                    
                    ; :else (do (println ls) ls))
                  ls))
              code)))