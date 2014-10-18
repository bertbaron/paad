(ns paad.functional-tests
  (:require [clojure.test :refer :all]
            [paad.core :as p])
  (:require [paad.examples.sliding-puzzle :as sp]))

(defn assert-result [cost expanded visited result]
  (is (= cost     (.cost (:node result))))
  (is (= expanded (:expanded result)))
  (is (= visited  (:visited result))))
         
(deftest eight-puzzle
  (let [s (sp/create-state [[-1  3  4] 
                            [ 2  0  1] 
                            [ 6  7  5]]) 
        g (sp/goal-fn 3)
        x sp/get-steps
        h sp/manhattan-distance]
    (testing "8-puzzle"
      (assert-result 12.0     27     15 (p/solve s g x :algorithm :A*   :constraint (p/no-return-constraint) :heuristic h))
      (assert-result 12.0     20     20 (p/solve s g x :algorithm :IDA* :constraint (p/no-return-constraint) :heuristic h))
      (assert-result 12.0     41     15 (p/solve s g x :algorithm :A*   :heuristic h))
      (assert-result 12.0     20     20 (p/solve s g x :algorithm :IDA* :heuristic h))
      (assert-result 12.0   3130   1805 (p/solve s g x :algorithm :A*   :constraint (p/no-return-constraint)))      
      (assert-result 12.0   4725   4732 (p/solve s g x :algorithm :IDA* :constraint (p/no-return-constraint)))
      (assert-result 12.0   2202   1369 (p/solve s g x :algorithm :A*   :constraint (p/create-cheapest-path-constraint)))      
      (assert-result 12.0   4091   4098 (p/solve s g x :algorithm :IDA* :constraint (p/create-cheapest-path-constraint)))
      (assert-result 12.0 369300 129314 (p/solve s g x :algorithm :A*))
      (assert-result 12.0 250437 250435 (p/solve s g x :algorithm :IDA*))     
      (assert-result 12.0 432294 152748 (p/solve s g x :algorithm :BF))
      (assert-result 14.0 239755 239739 (p/solve s g x :algorithm :DF   :limit 14))
      )))
