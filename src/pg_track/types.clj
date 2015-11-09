(ns pg-track.types
  (:require [pg-track.helpers :as h]))

(def column-types #{:char :varchar :integer :date})

(defprotocol TypeResolver
  (get-sql-string [this type] "Get sql representation of type.")
  (need-size? [this type] "Is type need size?")
  (is-type-valid? [this type] "Check that type is valid."))

(defn build-type-sql
  [[type size]]
  (str (clojure.core/name type)
       (when size (h/wrap-brackets size))))

(defrecord SimpleTypeResolver [types]
  TypeResolver
  (get-sql-string [_ type] (build-type-sql type))
  (need-size? [_ type] true)
  (is-type-valid? [_ [type]] (types type)))

(def resolver (->SimpleTypeResolver column-types))
