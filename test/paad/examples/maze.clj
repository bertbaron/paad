(ns paad.examples.maze)

(defrecord State [maze target pos])

(defn index-of [maze c]
  (let [indexed (map-indexed vector maze)
        r (map (fn [[x row]] [x (.indexOf row c)]) indexed)]
    (some #(if (>= (% 1) 0) % nil) r)))

(defn to-state [maze]
  (let [matrix (vec (map vec maze))
        pos    (index-of matrix \+)
        target (index-of matrix \*)
        matrix (assoc-in matrix (reverse pos) \space)]
    matrix))

(def test-maze [" +           "
                "## ###       "
                " # #         "
                "   # #     ##"
                "#### ####### "
                "            *"])