(ns pg-track.core-test
  (:require [expectations :refer [expect]]
            [pg-track.core :refer :all]))

;; Types test

(def simple-char (get-sql-string resolver [:char 5]))
(expect "char(5)" simple-char)

(expect nil? nil)
(expect {:a 1} (assoc {} :a 1))

(def tbl-name "films")
(def table-base (table tbl-name))

(expect {:name tbl-name :columns []} table-base)


(def id-column {:name "id" :type [:integer] :null? false})
(def code-column {:name "code" :type [:char 5] :null? true})
(def test-table {:name tbl-name
                 :columns [id-column code-column]})

(def test-table-dsl
  (-> (table tbl-name)
      (c-integer "id" false) 
      (c-char "code" 5)))

(expect test-table test-table-dsl)

(expect true (table-is-valid? test-table))
(expect false (table-is-valid? (add-column table-base "id" [:int] nil)))

;; remove last char
(expect "test" (remove-last-char "test,"))

(def sql (create-sql test-table))
;; (println sql)
;;(expect "" sql)

