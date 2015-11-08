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

(defn table*
  [name]
  {:name name
   :columns []})

(defn add-option
  [options option]
  (if (vector? option)
    (assoc options (option 0) (option 1))
    (assoc options option nil)))

(defn parse-options
  [options]
  (reduce add-option {} options))

(defn column*
  [table name type & options]
  (update-in 
   table 
   [:columns] 
   conj 
   {:name name
    :type type
    :options (parse-options options)}))



;; Check shemas

(defn table-is-valid?
  [{cs :columns}]
  (every? #(is-type-valid? resolver (:type %)) cs))

;; Generate sql

(defn wrap-brackets
  [s]
  (str "(" s ")"))

(def available-options
  {:primary-key (constantly "PRIMARY KEY")
   :not-null (constantly "NOT NULL")
   :default #(str "DEFAULT " %)
   :check #(str "CHECK " (wrap-brackets %))})

(defn option-sql
  [[type value]]
  ((available-options type) value))

(defn options-sql
  [options]
  (cljs/join " " (map option-sql options)))

(defn column-sql
  [{:keys [name type options]}]
  (str "\t" 
       name
       " "
       (get-sql-string resolver type)
       (when-not (empty? options) (str " " (options-sql options)))))

(defn remove-last-char
  [s] 
  (.substring s 0 (- (count s) 1)))

(defn create-sql
  [{:keys [name columns]}]
  (let [csql (->> columns (map column-sql) (cljs/join ",\n"))] 
    (str "CREATE TABLE " name " (\n" csql "\n);")))

