(ns pg-track.core-test
  (:require [expectations :refer [expect]]
            [pg-track.core :refer :all]))

;; Types test

(def simple-char (get-sql-string resolver [:char 5]))
(expect "char(5)" simple-char)

(expect nil? nil)
(expect {:a 1} (assoc {} :a 1))

(def tbl-name "films")
(def table-base (table* tbl-name))

(expect {:name tbl-name :columns []} table-base)


(def id-column {:name "id" :type [:integer] :options {:not-null nil}})
(def code-column {:name "code" :type [:char 5] :options nil})
(def test-table {:name tbl-name
                 :columns [id-column code-column]})

(def test-table-dsl
  (-> (table* tbl-name)
      (column* "id" [:integer] {:not-null nil}) 
      (column* "code" [:char 5] nil)))

(expect test-table test-table-dsl)

(expect true (table-is-valid? test-table))
(expect false (table-is-valid? (column* table-base "id" [:int] nil)))

;; remove last char
(expect "test" (remove-last-char "test,"))

;; options sql
(expect "PRIMARY KEY" (option-sql [:primary-key nil]))
(def opts {:primary-key nil :check "name <> ''" :not-null nil})
(expect "PRIMARY KEY CHECK (name <> '') NOT NULL" (options-sql opts))

(def sql (create-sql test-table))
;; (println sql)
;;(expect "" sql)

