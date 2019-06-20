(defproject service "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.6"]
                 [sonian/carica "1.1.0" :exclusions [[cheshire]]]
                 [korma "0.3.1"]
                 [mysql/mysql-connector-java "5.1.30"]
                 [fogus/ring-edn "0.2.0"]]
  :ring {:handler service.core/app})
