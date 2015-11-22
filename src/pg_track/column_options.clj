(ns pg-track.column-options
  (:refer-clojure :exclude [drop]))

(defrecord Null [])

(defrecord Default [value])

(defn find-option-by-type
  [options option]
  (let [t (type option)]
    (some  #(when (= t (type %)) %)  options)))

(defn make-reduce-value 
  [options]
  {:add #{} 
   :drop options
   :change #{}})

(defn drop-opt
  [r opt]
  (update r :drop disj opt))

(defn add-opt
  [r opt]
  (update r :add conj opt))

(defn change-opt
  [r from to]
  (update r :change conj {:from from :to to}))

(defn reduce-option
  "result is {:add #{}, :drop #{}, :change #{}}
   option is next option from new scheme
   on start in result in :drop options from old scheme"
  [{:keys [add drop change] :as r} new-opt]
  (if-let [old-opt (find-option-by-type drop new-opt)]
    (let [remove-from-drop (drop-opt r old-opt)] 
      (if (= new-opt old-opt)
       remove-from-drop
       (change-opt remove-from-drop old-opt new-opt)))
    (add-opt r new-opt)))

(defn options-diff
  [old-options new-options]
  (reduce reduce-option
          (make-reduce-value old-options)
          new-options))
