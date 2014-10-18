(ns paad.examples.peg-solitaire
  (:require [paad.core :as p])
  (:import [paad.core Node]))

(set! *warn-on-reflection* true)

(defrecord State [positions last-dest])

(defn on-board
  "Returns true if the position is on the board"
  ([[x y]] (on-board x y))
  ([x y] (or (and (<= 0 x 6) (<= 2 y 4))
             (and (<= 0 y 6) (<= 2 x 4)))))

(defn create-state
  "Creates a board with a single hole in the specified [x y] position"
  [holeposition]
  (let [all (set (for [x (range 0 7)
                       y (range 0 7)
                       :when (on-board x y)]
                      [x y]))]
    (State. (disj all holeposition) nil)))

(defn get-symbol [positions mark position]
  (cond (positions position) (if (= position mark) "◙ " "● ")
        (on-board position)  (if (= position mark) "◘ " "○ ")
        :default             "  "))

(defn print-state
  "Prints the state in a pretty way"
  ([state]
    (print-state state nil))
  ([state mark]
    (doseq [y (range 0 7)]
      (let [row (for [x (range 0 7)] [x y])]
        (println (apply str (map #(get-symbol (:positions state) mark %) row)))))))

(defn goal-fn [pinposition]
  (fn [state]
;    (println "testing " `(= #{~pinposition} (:positions ~state)))
    (= #{pinposition} (:positions state))))

(def symmetries [(fn [p] p)                      ; rotate   0°
                 (fn [[x y]] [(- 6 y) x])        ; rotate  90°
                 (fn [[x y]] [(- 6 x) (- 6 y)])  ; rotate 180°
                 (fn [[x y]] [y (- 6 x)])        ; rotate 270°
                 (fn [[x y]] [(- 6 x) y])        ; flip     0°
                 (fn [[x y]] [(- 6 y) (- 6 x)])  ; flip    90°
                 (fn [[x y]] [x (- 6 y)])        ; flip   180°
                 (fn [[x y]] [y x])])            ; flip   270°

(defn cannonical
  "Returns the same state for all 8 symmetric states. Used to optimize search."
  [state]
    (let [positions (:positions state)
          sorted    (sort-by first (map #(vector (vec (sort (map % positions))) %) symmetries))
          [positions symmetry] (first sorted)]
      (State. (into #{} positions) (symmetry (:last-dest state)))))

(defn move
  "Moves the peg at the specified position in the specified direction.
   Returns [true new-positions] for valid or [false nil] for invalid moves"
  [positions position direction]
  (let [delta        ({:up [0 -1] :down [0 1] :left [-1 0] :right [1 0]} direction)
        to-remove    (mapv + delta position)
        new-position (mapv + delta to-remove)
        valid        (and (positions to-remove)
                          (on-board new-position)
                          (not (positions new-position)))]
    [valid new-position (if valid (disj (conj positions new-position) position to-remove) nil)]))
        
(defn expand [state]
  (let [positions (:positions state)]
    (for [position  positions
          direction [:up :down :left :right]
          :let [[isvalid new-position newpositions] (move positions position direction)
                cost (if (= (:last-dest state) position) 0 1)
                cost (if (= (count newpositions) 1) (+ cost 1) cost)] ; Inrease cost for last step with 1 to make heuristic admissable
          :when isvalid]
      (do
;        (println "Expanding " [position direction cost])
        (p/step [position direction] (cannonical (State. newpositions new-position)) cost)))))

;(defn create-constraint
;  "A constraint that filters out duplicate states"
;  []
;  (let [cache (java.util.HashSet.)]
;    (fn constraint [^Node node]
;      (let [added (.add cache (cannonical (.state node)))]
;        (when (and added (= (rem (.size cache) 1000) 0)) (println "." (.size cache)))
;        (not added)))))

(defn print-solution [solution]
  (if (:node solution)
    (letfn [(print-path [^Node node mark]
              (when-let [parent (.parent node)]
                (print-path parent ((.operation node) 0)))
              (print-state (.state node) mark)
              (println))]
      (print-path (:node solution) nil))
    (println "No solution:" solution)))
    
(defn single-peg [state]
  (= 1 (count (:positions state))))

(defn heuristic [state]
  (/ (dec (count (:positions state))) 100))

(defn do-solve
  ([]
    (do-solve [3 3]))
  ([position]
    (when (not (on-board position)) (throw (RuntimeException. (str "Position " position " is not on the board"))))
    (let [s (create-state position)
;          g (goal-fn [3 3])
          g single-peg]
;      (let [solution (p/solve s g expand :algorithm :DF :constraint c :heuristic heuristic)]
      (let [solution (p/solve s g expand :algorithm :A* :constraint (p/cheapest-path-constraint) :heuristic heuristic :limit 9.0)]
        (print-solution solution)
        solution))))

(defn dump-memory []
  (let [runtime (java.lang.Runtime/getRuntime)]
    (doseq [x (range 3)]
      (System/gc)
      (Thread/sleep 100)
      (System/gc)
      (println (Math/round ^double (/ (- (.totalMemory runtime) (.freeMemory runtime)) 1024 1024)  )))))