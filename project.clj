(defproject pg-track "0.1.0-SNAPSHOT"
  :description "Postgresql migration and schema DSL tool."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.postgresql/postgresql "9.4-1205-jdbc41"]
                 [expectations "2.0.9"]
                 ]
  :plugins [[lein-expectations "0.0.7"]
            [lein-autoexpect "1.7.0"]])


