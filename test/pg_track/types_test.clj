(ns pg-track.types-test
  (:require [pg-track.types :refer :all]
            [expectations :refer [expect]]))

(def simple-char (get-sql-string resolver [:char 5]))
(expect "char(5)" simple-char)
