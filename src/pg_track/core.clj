(ns pg-track.core
  (:require [clojure.string :as cljs]
            [pg-track.helpers :as h]))

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
  "Args format: type-string (option-key (option-value) ...)
   Example: (column* tbl \"id\" \"integer\")
   (column* tbl \"id\" \"char(5\"))
   (column* tbl \"id\" \"integer\" :not-null)
   (column* tbl \"id\" \"char(5)\" :not-null)"
  [table name type & options]
  (let [options-v (parse-options options)
        column {:name name :type type :options options-v}]
    (update-in table [:columns] conj column)))

(defn extract-columns
  [columns]
  (->> columns
       (reduce (fn [{:keys [last cs] :as r} token] 
                 (if (< (count last) 2)
                   (update-in r [:last] conj token)
                   (if (string? token)
                     {:last [token] :cs (conj cs last)}
                     (update-in r [:last] conj token)))) 
               {:last [] :cs []})
       ((fn [{:keys [last cs]}] (conj cs last)))))

(defn table
  "Columns need to be in format string column name, when type keyword, when optional type size, when optional options."
  [name & columns]
  (let [cs (extract-columns columns)
        tbl (table* name)]
    (reduce (partial apply column*) tbl cs)))

;; Generate sql

(def available-options
  {:primary-key (constantly "PRIMARY KEY")
   :not-null (constantly "NOT NULL")
   :default #(str "DEFAULT " %)
   :check #(str "CHECK " (h/wrap-brackets %))})

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
       type
       (when-not (empty? options) (str " " (options-sql options)))))

(defn remove-last-char
  [s] 
  (.substring s 0 (- (count s) 1)))

(defn create-sql
  [{:keys [name columns]}]
  (let [csql (->> columns (map column-sql) (cljs/join ",\n"))] 
    (str "CREATE TABLE " name " (\n" csql "\n);")))

