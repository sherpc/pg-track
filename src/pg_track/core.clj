(ns pg-track.core
  (:require [clojure.java.jdbc :as j]))

(def connection-uri "")

(def db {:subprotocol "postgresql"
         :subname "//127.0.0.1:5432/test"
         :user "postgres"
         :password "postgres"})

(def row {:id 1 :name "Tester"})


