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

(def id-options {:primary-key nil, :default "nextval('serial')"})
(def id-column {:name "did" :type [:integer] :options id-options})
(def code-column {:name "code" :type [:char 5] :options {}})
(def title-options {:not-null nil, :check "title <> ''"})
(def title-column {:name "title" :type [:varchar 40] :options title-options})
(def test-table {:name tbl-name
                 :columns [code-column title-column id-column]})

(def test-table-dsl
  (-> (table* "films")
      (column* "code" [:char 5])
      (column* "title" [:varchar 40] :not-null [:check "title <> ''"])
      (column* "did" [:integer] :primary-key [:default "nextval('serial')"])))

(expect test-table test-table-dsl)

(expect true (table-is-valid? test-table))
(expect false (table-is-valid? (column* table-base "id" [:int])))

;; remove last char
(expect "test" (remove-last-char "test,"))

;; options sql
(expect "PRIMARY KEY" (option-sql [:primary-key nil]))
(def opts {:primary-key nil :check "name <> ''" :not-null nil})
(expect "PRIMARY KEY CHECK (name <> '') NOT NULL" (options-sql opts))

;; options dsl
(expect {:not-null nil} (parse-options [:not-null]))
(expect opts (parse-options [:primary-key :not-null [:check "name <> ''"]]))

(def sql (create-sql test-table-dsl))
(println sql)
;;(expect "" sql)

