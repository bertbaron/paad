(ns paad.core-test
  (:require [clojure.test :refer :all]
            [paad.core :refer :all])
  (:import [paad.core Node Step]))

(set! *warn-on-reflection* true)

(defn mapply [f & args] (apply f (apply concat (butlast args) (last args))))

(def tree {:a [[:b 1] [:c 1]] 
           :b [[:D 1] [:c 1]]})

(defn is-goal [keyword]
  (java.lang.Character/isUpperCase ^char (first (name keyword))))

(defn tree-expand [tree]
  (fn inner-expand [keyword]
    (map-indexed
      (fn [idx [state cost]] (Step. idx state cost)) (keyword tree))))

(defn to-actual [result]
  (map #(vector (.state ^Node (:node %)) (.cost ^Node (:node %))) result))

(defn test-solve-all [tree expected & {:keys [constraint limit] :as options}]
  (doseq [algorithm [:A* :BF :IDA* :DF]]
    (testing (str algorithm " for " tree)
      (let [result (mapply solve :a is-goal (tree-expand tree) :algorithm :A* :all true options)]
        (cond (#{:A* :BF} algorithm) (is (= expected (to-actual result)))
              (#{:IDA*}   algorithm) (is (= (first expected) (first (to-actual result))))
              (#{:DF}     algorithm) (is (= (sort expected) (sort (to-actual result)))))))))

(defn test-statistics [tree algorithm expected & {:keys [constraint] :as options}]
  (testing (str algorithm " statistics for " tree)
    (let [result (mapply solve :a is-goal (tree-expand tree) :algorithm algorithm options)]
      (is (= expected (select-keys result [:visited :expanded]))))))

(deftest simple-test
  (let [graph {:a [[:b 1] [:c 1]] 
               :b [[:D 1] [:c 1]]}]
    (test-solve-all graph [[:D 2.0]])))

(deftest optimal-even-if-path-looks-bad
  (let [graph {:a  [[:b 1] [:c 8] [:d 10]]
               :b  [[:bb 1]]
               :c  [[:cc 8]]
               :d  [[:dd 10]]
               :bb [[:B 200]]
               :cc [[:C 100]]
               :dd [[:D 1]]}]
    (test-solve-all graph [[:D 21.0] [:C 116.0] [:B 202.0]])))
  
(deftest test-with-no-return-constraint
  (let [graph {:a  [[:a 1] [:b 1]]
               :b  [[:a 1] [:B 1]]}]
    (test-solve-all graph [[:B 2.0]] :constraint (no-return-constraint))
    (test-statistics graph :A* {:expanded 8 :visited 4})    
    (test-statistics graph :A* {:expanded 2 :visited 2} :constraint (no-return-constraint))))

(deftest test-with-no-loop-constraint
  (let [graph {:a  [[:a 1] [:b 1]]
               :b  [[:c 1] [:d 2]]
               :c  [[:a 1] [:d 1]]
               :d  [[:E 1]]}]
    (test-statistics graph :A* {:expanded 22 :visited 12})    
    (test-statistics graph :A* {:expanded  8 :visited  6} :constraint (no-return-constraint))
    (test-statistics graph :A* {:expanded  6 :visited  5} :constraint (no-loop-constraint))))

;parent state operation ^double cost ^double value
(defn node [state value]
  (Node. nil state  nil 0 value))

(deftest concurrent-replace-test
  (let [concurrent-replace (ns-resolve 'paad.core 'concurrent-replace)
        map (java.util.concurrent.ConcurrentHashMap.)]
    (testing "concurrent-replace"
      (is (= true (concurrent-replace map :a nil 1)))
      (is (= false (concurrent-replace map :a nil 2)))
      (is (= false (concurrent-replace map :a 2 3)))
      (is (= true (concurrent-replace map :a 1 4)))
      )))

(deftest test-cheapest-path-constraint
  (let [p ((create-cheapest-path-constraint))]
    (testing "on-expand only"
      (is (= false (on-expand p (node :a 2))))
      (is (= true (on-expand p (node :a 2))))
      (is (= true (on-expand p (node :a 3))))
      (is (= false (on-expand p (node :a 1)))))

   (testing "on-expand and on-visit only"
     (is (= false (on-expand p (node :b 2))))
     (is (= false (on-expand p (node :b 1))))
     (is (= true  (on-visit  p (node :b 2))))
     (is (= false (on-visit  p (node :b 1))))
     (is (= true  (on-visit  p (node :b 1))))
     )
      ))
    