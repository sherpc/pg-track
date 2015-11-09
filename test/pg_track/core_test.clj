(ns pg-track.core-test
  (:require [expectations :refer [expect]]
            [pg-track.core :refer :all]))

(def tbl-name "films")
(def table-base (table* tbl-name))

(expect {:name tbl-name :columns []} table-base)

(def id-options {:primary-key nil, :default "nextval('serial')"})
(def id-column-base {:name "did" :type "integer" :options {}})
(def id-column {:name "did" :type "integer" :options id-options})
(def code-column {:name "code" :type "char(5)" :options {}})
(def title-opts {:not-null nil, :check "title <> ''"})
(def title-column {:name "title" :type "varchar(40)" :options title-opts})
(def date-column {:name "date_prod" :type "date" :options {}})
(def test-table {:name tbl-name
                 :columns [code-column title-column id-column date-column]})

(def simple-dsl 
  (table tbl-name
        "code" "char(5)"
        "title" "varchar(40)" :not-null [:check "title <> ''"]
        "did" "integer" :primary-key [:default "nextval('serial')"]
        "date_prod" "date"))

(def test-table-dsl
  (-> table-base
      (column* "code" "char(5)")
      (column* "title" "varchar(40)" :not-null [:check "title <> ''"])
      (column* "did" "integer" :primary-key [:default "nextval('serial')"])
      (column* "date_prod" "date")))

(expect {:name tbl-name :columns [code-column]} (column* table-base "code" "char(5)"))
(expect {:name tbl-name :columns [id-column-base]} (column* table-base "did" "integer"))
(expect {:name tbl-name :columns [id-column]} (column* table-base "did" "integer" :primary-key [:default "nextval('serial')"]))

(expect test-table test-table-dsl)

;; remove last char
(expect "test" (remove-last-char "test,"))

;; options sql
(expect "PRIMARY KEY" (option-sql [:primary-key nil]))

(def opts {:primary-key nil :check "name <> ''" :not-null nil})

(expect 
 "PRIMARY KEY CHECK (name <> '') NOT NULL" 
 (options-sql opts))

;; options dsl
(expect {:not-null nil} (parse-options [:not-null]))
(expect opts (parse-options [:primary-key :not-null [:check "name <> ''"]]))

;; table dsl

(expect 
 [["code" "char(5)"]] 
 (extract-columns ["code" "char(5)"]))

(expect 
 [["code" "char(5)"] ["date_prod" "date"]]
 (extract-columns ["code" "char(5)" "date_prod" "date"]))

(expect 
 {:name tbl-name :columns [code-column]} 
 (table tbl-name "code" "char(5)"))

(expect 
 {:name tbl-name :columns [code-column id-column]} 
 (table tbl-name "code" "char(5)" "did" "integer" :primary-key [:default "nextval('serial')"]))

(expect test-table-dsl simple-dsl)

;; (def sql (create-sql simple-dsl))
;; (println sql)
