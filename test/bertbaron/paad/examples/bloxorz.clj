(ns bertbaron.paad.examples.bloxorz
  "Solves a simplified version of the game Bloxorz (http://www.coolmath-games.com/0-bloxorz) 
   with no switches, teleport tiles or orange tiles. Theres only simple tiles, a block and a
   target hole.
   As such, the state consists only of the block position, which is simply represented as
   [[row1 col1] [row2 col2]]."
  (:require [bertbaron.paad.core :as p]))

; terrain is a function from position to the field (i.e. :target or :tile) or nil if there is no field
(defrecord Game [terrain startpos])

(defn parse 
  "Parses a game from a vector of strings"
  [level]
  (let [tokens        {\S :start
                       \T :target
                       \o :tile
                       \c :tile} ;TODO :orange, for tiles on which the block can not stand right up
        level         (vec (for [row level]
                             (vec (map tokens row))))
        terrain       (fn [pos] (get-in level pos))
        indexed       (map-indexed vector level)
        [row rowvec]  (first (filter #(>= (.indexOf (% 1) :start) 0) indexed))
        startpos [row (.indexOf rowvec :start)]]
    (->Game terrain startpos)))

(defn infinte
  "Creates a game on an infinite field with the given start and target position"
  [startpos targetpos]
  (->Game (fn [pos]
            (if (= targetpos pos) :target :tile))
           startpos))

(defn standing?
  "Returns true if the block is standing"
  [[b1 b2]]
  (= b1 b2))

(defn valid?
  "Returns true if the block position is valid in the given game"
  [game [b1 b2]]
  (and ((:terrain game) b1)
       ((:terrain game) b2)))

(defn dx [[row col] d]
  [row (+ col d)])

(defn dy [[row col] d]
  [(+ row d) col])

(defn bdx [[b1 b2] d1 d2]
  [(dx b1 d1) (dx b2 d2)])

(defn bdy [[b1 b2] d1 d2]
  [(dy b1 d1) (dy b2 d2)])

(defn move
  "Returns the new block position after applying the move in the given direction (:up :down :left or :right)
   We ensure that b1 is always smaller than b2"
  [[b1 b2 :as block] dir]
  (let [standing (standing? block)
        vertical (= (b1 1) (b2 1))]
    (case dir
      :up    (cond
               standing (bdy block -2 -1)
               vertical (bdy block -1 -2)
               :default (bdy block -1 -1))
      :down  (cond
               standing (bdy block 1 2)
               vertical (bdy block 2 1)
               :default (bdy block 1 1))
      :left  (cond
               standing (bdx block -2 -1)
               vertical (bdx block -1 -1)
               :default (bdx block -1 -2))
      :right (cond
               standing (bdx block 1 2)
               vertical (bdx block 1 1)
               :default (bdx block 2 1)))))

(defn startstate [game]
  [(:startpos game) (:startpos game)])

(defn goal-fn [game]
  (fn [[b1 b2 :as block]] (and (= b1 b2)
                               (= :target ((:terrain game) b1)))))

(defn step
  "Returns the step for the given move, or nil if the move is invalid"
  [game block dir]
  (let [moved (move block dir)]
    (if (valid? game moved) 
      (p/step dir moved 1.0))))

(defn expand-fn [game]
  (fn [block]
    (keep #(step game block %) [:up :down :left :right])))

(defn solve [game]
  (let [result (p/solve (startstate game) (goal-fn game) (expand-fn game)
                        :algorithm  :BF
                        :constraint (p/cheapest-path-constraint))]
    (println (:statistics result) (-> result :solution last :cost))
    (map :operation (rest (:solution result)))))

(def level0 (parse ["      "
                    "  ST  "
                    "  oo  "
                    "  oo  "
                    "      "]))

(def level1 (parse ["ooo       "
                    "oSoooo    "
                    "ooooooooo "
                    " ooooooooo"
                    "     ooToo"
                    "      ooo "]))

(def level3 (parse ["      ooooooo "
                    "oooo  ooo  oo"
                    "ooooooooo  oooo"
                    "oSoo       ooTo"
                    "oooo       oooo"
                    "            ooo"]))

(def level4 (parse ["   ccccccc"
                    "   ccccccc"
                    "oooo     ooo"
                    "ooo       oo"
                    "ooo       oo"
                    "oSo  ooooccccc"
                    "ooo  ooooccccc"
                    "     oTo  ccoc"
                    "     ooo  cccc"]))

(def infinite1 (infinte [0 0] [1 1]))
(def infinite2 (infinte [0 0] [40 50]))

;(solve level1)