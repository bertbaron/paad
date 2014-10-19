(ns paad.examples.sliding-puzzle
  (:require [paad.core :as p])
  (:require primitive-math))

(primitive-math/use-primitive-operators)

(set! *warn-on-reflection* true)

; some debugging/timing functions

(def elapsed (atom 0))

(defn reset-timer []
  (reset! elapsed 0))

(defmacro do-time [form]
  `(let [start#  (java.lang.System/nanoTime)
         result# ~form
         end#    (java.lang.System/nanoTime)
         time#   (- end# start#)]
     (swap! elapsed clojure.core/+ time#)
     result#))

; end of debugging code

(deftype State [board ^long size ^long x ^long y]
  Object
  (equals [a b] (= (.board a) (.board ^State b)))
  (hashCode [this] (.hashCode (.board this))))

(defn print-state [^State state]
  (doseq [row (partition (.size state) (.board state))]
    (println (apply str (map #(if % (format " %2d " %) "    ") row))))
  (println (.size state) (.x state) (.y state)))

(defmacro abs [x]
  `(long (Math/abs (long ~x))))

(defmacro index [x y size]
  `(+ ~x (* ~y ~size)))

(defmacro get-x [idx size]
  `(rem ~idx ~size))

(defmacro get-y [idx size]
  `(/ ~idx ~size))

(defmacro abs-diff [a b]
  `(abs (- ~a ~b)))

(defmacro dist [idx1 idx2 size]
  `(+ (abs-diff (get-x ~idx1 ~size) (get-x ~idx2 ~size))
      (abs-diff (get-y ~idx1 ~size) (get-y ~idx2 ~size))))

(defn manhattan-distance [^State state]
  (let [board (.board state)
        size  (.size state)]
    (loop [i (dec (count board))
           d 0]
      (if (< i 0)
        d
       (recur
         (- i 1)
         (+ d (let [v (long (board i))]
                (if (>= v 0)
                  (dist i v size)
                  0))))))))

(defn swap-elements [v i j]
  (assoc v i (v j) j (v i)))

(defn move [^State state ^long dx ^long dy]
  (let [board   (.board state)
        size    (.size state)
        x       (.x state)
        y       (.y state)
        nx      (+ x dx)
        ny      (+ y dy)
        old     (index x y size)
        new     (index nx ny size)
        nb      (swap-elements board old new)]
    (State. nb size nx ny)))

(defn get-steps [^State state]
  (do-time
  (filter identity [(when (> (.x state) 0)                   (p/step :left  (move state -1  0) 1.0))
                    (when (> (.y state) 0)                   (p/step :up    (move state  0 -1) 1.0))
                    (when (< (.x state) (dec (.size state))) (p/step :right (move state  1  0) 1.0))
                    (when (< (.y state) (dec (.size state))) (p/step :down  (move state  0  1) 1.0))]))
  )

(defn expand [state]
  (map #(:state %) (get-steps state)))

(defn create-initial-state [^long size]
  (let [board (assoc (vec (range (* size size))) (dec (* size size)) -1)
        max (dec size)]
    (State. board size max max)))

(defn create-state [rows]
  (let [size  (long (count rows))
        board (vec (flatten rows))
        idx   (long (.indexOf ^java.util.List board -1))]
    (State. board size (get-x idx size) (get-y idx size))))
        
(defn goal-fn [size]
  (let [solved-state (create-initial-state size)
        goal-board   (.board ^State solved-state)]
    (fn [^State state]
      (let [goal (= goal-board (.board state))]
        goal))))

(defn random-move [state]
  (first (shuffle (expand state))))

(defn shuffle-state [state iterations]
  (nth (iterate random-move state) iterations))

(def s (create-initial-state 4))
(def t (shuffle-state s 200))

;(def test-state (State. [ 4  13   5  11
;                          2   0   8   3
;                         10   1  12   7
;                         14  -1   6   9]
;                        4 1 3)) ; 50

(def test-state (State. [   2  12   0   1 
                            4  11   6   3 
                           -1  13   8  10 
                            5   9  14   7] 
                        4 0 2)) ; 40

;(def test-state (State. [  0   1   2   3 
;                           4   5  -1   6 
;                           8   9  10   7 
;                          12  13  14  11 ]
;                        4 2 1)) ; 3

(defn print-solution [result]
  (let [solution (:solution result)]
    (println "STATISTICS:" (:statistics result))
    (if solution
      (let [ops (map :operation (rest solution))]
        (println (-> solution last :cost) ":" ops)
        ops)
      (do
        (println "No solution")
        []))))

(defn do-solve []
  (p/solve test-state (goal-fn 4) get-steps
           :algorithm :IDA*
;           :limit 7
          :constraint (p/no-return-constraint)
           :heuristic manhattan-distance))

