(ns pg-track.core
  (:require [clojure.java.jdbc :as j]
            [clojure.string :as cljs]))

(def db {:subprotocol "postgresql"
         :subname "//127.0.0.1:5432/test"
         :user "postgres"
         :password "postgres"})

(def row {:id 1 :name "Tester"})

(def column-types #{:char :varchar :integer :date})

(defn table
  [name]
  {:name name
   :columns []})

(defn add-column
  [table name type size null?]
  (update-in 
   table 
   [:columns] 
   conj 
   {:name name
    :type type
    :size size
    :null? null?}))

(defn c-integer
  ([table name] (c-integer table name true))
  ([table name null?]
   (add-column table name :integer nil null?)))

(defn c-char
  ([table name size] (c-char table name size true))
  ([table name size null?]
   (add-column table name :char size null?)))

(defn table-is-valid?
  [table]
  (not (some #(not (column-types (:type %))) (:columns table))))

(defn column-sql
  [{:keys [name type size null?]}]
  (str name
       " "
       (clojure.core/name type)
       (when size (str "(" size ")"))))

(defn remove-last-char
  [s] 
  (.substring s 0 (- (count s) 1)))

(defn create-sql
  [{:keys [name columns]}]
  (let [csql (->> columns (map column-sql) (cljs/join ",\n"))] 
    (str "CREATE TABLE " name " (\n" csql "\n);")))
