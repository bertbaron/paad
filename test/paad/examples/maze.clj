(ns paad.examples.maze
  (:require [paad.core :as p]))

(defrecord State [maze x y target])

(defn char-at
  "Returns the character at the specified position in the maze"
  [maze x y]
  (.charAt (maze y) x))

(defn index-of
  "Returns the [x y] index of the first occurence of character c in the maze"
  [maze c]
  (first (filter identity (for [y (range 0 (count maze))]
                            (let [x (.indexOf ^String (maze y) (str c))]
                              (if (>= x 0) [x y]))))))

(defn to-state 
  "Creates an initial state given a vector of strings representing the maze"
  [maze]
  (let [pos    (index-of maze \+)
        target (index-of maze \*)]
    (State. maze (pos 0) (pos 1) target)))

(defn move
  "Returns the new state by moving with the specified deltas,
   or nil if the move is invalid"
  [state Δx Δy]
  (let [maze (:maze state)
        x    (+ (:x state) Δx)
        y    (+ (:y state) Δy)]
    (if (and (< -1 y (count maze))
             (< -1 x (count (maze y)))
             (not= \# (char-at maze x y)))
      (State. maze x y (:target state))
      nil)))
  
(defn expand [state]
   (filter :state [(p/step :left  (move state -1  0) 1.0)
                   (p/step :right (move state  1  0) 1.0)
                   (p/step :up    (move state  0 -1) 1.0)
                   (p/step :down  (move state  0  1) 1.0)]))

(defn goal? [state]
  (= [(:x state) (:y state)] (:target state)))

(defn manhattan-distance [state]
  (+ (Math/abs (- (:x state) ((:target state) 0)))
     (Math/abs (- (:y state) ((:target state) 1)))))

(def test-maze [" +           "
                "#   ##       "
                " # #      ###"
                "   # #   #*  "
                "###   ###### "
                "             "])

(defn do-solve []
  (let [result (p/solve (to-state test-maze) goal? expand
                           :algorithm :A*
                           :heuristic manhattan-distance
                           :constraint (p/cheapest-path-constraint)
                           )]
    (println "visited" (:visited result) "expanded" (:expanded result) "cost" (:cost (:node result)))
    (p/get-operations (:node result))))
