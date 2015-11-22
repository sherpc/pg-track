(ns pg-track.column-options-test
  (:require [expectations :refer [expect]]
            [pg-track.column-options :refer :all]))

(expect 1 (->> [(->Null) (->Null)]
               (into #{})
               count))

(def null (->Null))
(def default-10 (->Default 10))
(def default-9 (->Default 9))

(def ops #{null default-10})

(expect 2 (count ops))
(expect false (= default-10 default-9))

;; -----
;; find-option-by-type
;; -----

(expect nil (find-option-by-type #{null} default-10))
(expect null (find-option-by-type ops null))
;; different values, same type
(expect default-10 (find-option-by-type ops default-9))

;; -----
;; reduce-value
;; -----

(def reduce-value (make-reduce-value ops))

;; add new option
(expect 
 {:add #{default-10} :drop #{null} :change #{}}
 (reduce-option (make-reduce-value #{null}) default-10))

;; remove from drop option (option not changed)
(expect
 {:add #{} :drop #{} :change #{}}
 (reduce-option (make-reduce-value #{null}) null))

;; change option

(expect 
 {:add #{} :drop #{} :change #{{:from default-9 :to default-10}}}
 (reduce-option (make-reduce-value #{default-9}) default-10))

;; -----
;; options-diff
;; -----

;; nothing changed
(expect 
 {:add #{} :drop #{} :change #{}}
 (options-diff ops ops))

;; new option
(expect
 {:add #{null} :drop #{} :change #{}}
 (options-diff #{default-10} ops))

;; drop option
(expect
 {:add #{} :drop #{null} :change #{}}
 (options-diff ops #{default-10}))

;; change option
(expect
 {:add #{} :drop #{} :change #{{:from default-10 :to default-9}}}
 (options-diff ops #{null default-9}))
