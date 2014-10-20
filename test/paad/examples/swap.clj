(ns paad.examples.swaps
  (:require [paad.core :as p])
  (:require [clojure.pprint :as pp]))

;
; Sorts a vector of elements by swapping neighbours. The best solution is the
; one that requires the minimum number of swaps
;

(set! *warn-on-reflection* true)

(defn swap
  "Swaps element at index i in vector v with its right neighbour"
  [v i]
  (assoc v i (v (inc i)) (inc i) (v i)))

(defn expand [state]
  (for [i (range 0 (dec (count state)))]
    (p/step i (swap state i) 1.0)))

(defn goal? [state]
  (= state (sort state)))

(defn goal [target]
  (fn [state]
    (= state target)))

(defn heuristic [target]
  (let [; pre-calculate map from element to index in target vector
        indices (->> target (map-indexed vector) (map reverse) (map vec) (into {}))]
	  (fn [state]
      (loop [idx (dec (count state))
             acc 0]
        (if (< idx 0)
          (/ acc 2)
          (let [item (state idx)
                dist (Math/abs (- idx (long (indices item))))]
            (recur (dec idx) (+ acc dist))))))))
             
;		  (let [displacement (reduce + (for [item state]
;		                                 (Math/abs (- (.indexOf state item) (.indexOf target item)))))]
;		    (/ displacement 2)))))

(defn old-heuristic [target]
  (fn heuristic [state]
    (let [displacement (reduce + (for [item state]
                                   (Math/abs (- (.indexOf ^java.util.List state item) (.indexOf ^java.util.List target item)))))]
      (/ displacement 2))))

;(clojure.pprint/pprint (p/solve [5 4 3 2 1] goal? expand)) 

(defn print-result [result]
  (println (str (:statistics result) ", " (-> result :solution last :cost))))

(defn do-solve [state]
  (let [target (sort state)
        h-fn   (heuristic target)
        g-fn   (goal target)]
    (:statistics (p/solve state g-fn expand
                          :heuristic h-fn
;             :constraint (p/cheapest-path-constraint)
             ))))
    
;(:statistics (p/solve [5 4 3 2 1] goal? expand
;                      :constraint (p/cheapest-path-constraint)
;                      :algorithm :A*
;             ))

