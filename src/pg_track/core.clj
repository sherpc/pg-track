(ns pg-track.core
  (:require [clojure.java.jdbc :as j]
            [clojure.string :as cljs]))

(def db {:subprotocol "postgresql"
         :subname "//127.0.0.1:5432/test"
         :user "postgres"
         :password "postgres"})

(def row {:id 1 :name "Tester"})

;; Types

(def column-types #{:char :varchar :integer :date})

(defprotocol TypeResolver
  (get-sql-string [this type] "Get sql representation of type")
  (is-type-valid? [this type]))

(defn build-type-sql
  [[type size]]
  (str (clojure.core/name type)
       (when size (str "(" size ")"))))

(defrecord SimpleTypeResolver [types]
  TypeResolver
  (get-sql-string [_ type] (build-type-sql type))
  (is-type-valid? [_ [type]] (types type)))

(def resolver (->SimpleTypeResolver column-types))

;; Table

(defn table
  [name]
  {:name name
   :columns []})

(defn add-column
  [table name type null?]
  (update-in 
   table 
   [:columns] 
   conj 
   {:name name
    :type type
    :null? null?}))

(defn c-integer
  ([table name] (c-integer table name true))
  ([table name null?]
   (add-column table name [:integer] null?)))

(defn c-char
  ([table name size] (c-char table name size true))
  ([table name size null?]
   (add-column table name [:char size] null?)))

(def not-null false)

;; Check shemas

(defn table-is-valid?
  [{cs :columns}]
  (every? #(is-type-valid? resolver (:type %)) cs))

;; Generate sql

(defn column-sql
  [{:keys [name type null?]}]
  (str "\t" 
       name
       " "
       (get-sql-string resolver type)
       (when-not null? " NOT NULL")))

(defn remove-last-char
  [s] 
  (.substring s 0 (- (count s) 1)))

(defn create-sql
  [{:keys [name columns]}]
  (let [csql (->> columns (map column-sql) (cljs/join ",\n"))] 
    (str "CREATE TABLE " name " (\n" csql "\n);")))


#_(println
 (create-sql
  (-> (table "films")
      (c-char "code" 5)
      (add-column "title" [:varchar 40] not-null)
      (c-integer "did" not-null))))
