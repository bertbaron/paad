(ns paad.performance
  (:require [paad.core :as p])
  (:import [paad.core Node Step]))

(set! *warn-on-reflection* true)

(defn full-tree-test [algorithm width limit]
  (let [g (constantly false)
        steps (vec (map (fn [_] (Step. :op :state 1)) (range width)))
        expand (constantly steps)]
    (p/solve :state g expand :algorithm algorithm :limit limit)))

(defn average [coll] 
  (/ (reduce + coll) (count coll)))

(defn get-time [op]
  (let [start (System/nanoTime)]
    (op)
    (- (System/nanoTime) start)))
  
(defn multitime [op]
  (let [timings (force (for [i (range 6)] (get-time op)))
        avg     (->> timings rest sort rest butlast average)]
    (Math/round (double (/ avg 1000 1000)))))
    
(defmacro do-test [op]
  `(let [time# (multitime (fn [] ~op))]
     (println (str '~op "\t" time# "ms"))))

(defn do-tests []
  (do-test (full-tree-test :DF  2 20))
  (do-test (full-tree-test :DF 20  4))
  (do-test (full-tree-test :A*  2 18)))