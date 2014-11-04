(ns paad.swaps
  (:require [domina :as dom]
            [domina.events :as ev]
            [paad.core :as p]))

;
; Sorts a vector of elements by swapping neighbours. The best solution is the
; one that requires the minimum number of swaps
;

(defn swap
  "Swaps element at index i in vector v with its right neighbour"
  [v i]
  (assoc v i (v (inc i)) (inc i) (v i)))

(defn expand [state]
  (for [i (range 0 (dec (count state)))]
    (p/step i (swap state i) 1.0)))

(defn goal? [state]
  (= state (sort state)))

(defn abs [n] (max n (- n)))

(defn index-of [s item]
  (loop [i 0
         s (seq s)]
    (if s
      (if (= (first s) item)
        i
        (recur (inc i) (next s)))
      -1)))

(defn heuristic [state]
  (let [target (sort state)
        displacement (reduce + (for [item state]
                                 (abs (- (index-of state item)
                                         (index-of target item)))))]
    (/ displacement 2)))

(defn do-solve [state]
    (:statistics (p/solve state goal? expand
                          :heuristic heuristic)))
    
(defn solve []
  (let [input (dom/value (dom/by-id "input"))]
    (dom/set-value! (dom/by-id "result") (do-solve (vec input)))))

(defn ^:export init []
  (when (and js/document
             (.-getElementById js/document))
    (ev/listen! (dom/by-id "solve") :click solve)))
