(ns paad.core
  (:require primitive-math))

(defrecord Step [operation state ^double cost])
(def step ->Step)

(defrecord Node [parent state operation ^double cost ^double value])

(defprotocol Constraint
  "A possibly mutable constraint, returns true if a node is constraint, so it should not be expanded further."
  (on-expand [this node])
  (on-visit  [this node]))

(defrecord StatelessConstraint [constraint-fn]
  Constraint
  (on-expand [_ node] (constraint-fn node))
  (on-visit  [_ node] false))

(defn no-constraint []
  (fn [] (StatelessConstraint. (constantly false))))

(defn no-return-constraint []
  (letfn [(constraint [^Node node] (when-let [parent ^Node (.parent node)]
                                     (if (= (.state parent) (.state node))
                                       true
                                       (when-let [grandparent ^Node (.parent parent)]
                                         (= (.state grandparent) (.state node))))))]
    (fn [] (StatelessConstraint. constraint))))

(defn no-loop-constraint []
  (letfn [(constraint [^Node node] (let [state (.state node)]
                                     (loop [n ^Node (.parent node)]
                                       (when n
                                         (if (= state (.state n))
                                           true
                                           (recur (.parent n)))))))]
    (fn [] (StatelessConstraint. constraint))))

(defn- concurrent-replace [^java.util.concurrent.ConcurrentMap map key old-value new-value]
  (if old-value
    (.replace map key old-value new-value)
    (not (.putIfAbsent map key new-value))))

(defn- cond-update
  "Atomically puts the value in the map if the key is not mapped yet or if (cond old-value value) yields true
   Returns true if the values was put in the map, false if the condition did not hold"
  [^java.util.concurrent.ConcurrentMap map key value cond]
  (loop []
    (let [current (.get map key)]
      (if (or (not current) (cond current value))
        (concurrent-replace map key current value)))))
        
(defrecord CheapestPathToStateConstraint [^java.util.concurrent.ConcurrentMap map keyfn]
  ; Thread-safe mutable cheapest-path-to-state constraint backed by a ConcurrentMap
  ; The value in the map is a vector of [node-value is-visited]
  Constraint
  (on-expand [_ node]
    (not (cond-update map
                      (keyfn (.state ^Node node))
                      [(.value ^Node node) false]
                      (fn [current nw]
                        (< ^double (nw 0) ^double (current 0))))))

  (on-visit [_ node]
    (not (cond-update map
                      (keyfn (.state ^Node node))
                      [(.value ^Node node) true]
                      (fn [current nw]
                        (or (< ^double (nw 0) ^double (current 0))
                          (and (= (nw 0) (current 0))
                               (not (current 1)))))))))

(defn cheapest-path-constraint
  ([]
    (cheapest-path-constraint identity))
  ([keyfn]
    (fn []
      (CheapestPathToStateConstraint. (java.util.concurrent.ConcurrentHashMap.) keyfn))))

(defprotocol Strategy
  "A mutable or immutable queue used for storing expanded nodes for later evaluation."
  (s-conj! [this node])
  (s-peek  [this])
  (s-pop!  [this]))

; "An mutable strategy for depth-first and IDA*. May be more performant than immutable List or Vector."
(extend-type java.util.ArrayList
  Strategy
  (s-conj! [this node] (.add this node) this)
  (s-peek  [this] (if (> (.size this) 0) (.get this (dec (.size this))) nil))
  (s-pop!  [this] (.remove this (dec (.size this))) this))

; "A mutable strategy, use PriorityQueue for A* and LinkedList for breadth-first"
(extend-type java.util.Queue
  Strategy
  (s-conj! [this node] (.add this node) this)
  (s-peek  [this] (.peek this))
  (s-pop!  [this] (.poll this) this))

(defn- node-comparator [^Node n1 ^Node n2]
  (let [on-value (compare (.value n1) (.value n2))]
    (if (= 0 on-value) (compare (.cost n2) (.cost n1)) on-value)))

(defn- create-A*-strategy [] (java.util.PriorityQueue. 64 node-comparator))
(defn- create-df-strategy [] (java.util.ArrayList.))
(defn- create-bf-strategy [] (java.util.LinkedList.))

(defn- merge-result [old new]
  (merge new (apply (partial merge-with #(+ % %2)) (map #(select-keys % [:visited :expanded]) [old new]))))

(defn- node-2-solution [node]
  (if node
    (conj (node-2-solution (:parent node)) (select-keys node [:operation :state :cost]))
    []))

(defn- create-public-result [result]
  (let [stats    (select-keys result [:visited :expanded])
        solution (if-let [node (:node result)]
                   (node-2-solution node)
                   nil)]
    {:statistics stats :solution solution}))

(defn- expand-node [^Node node h-fn ^Step step]
  (let [new-state (.state step) #_(apply-fn (.state node) (.operation step))
        new-cost  (+ (.cost node) (.cost step))
        new-value (max (.value node) (+ new-cost ^double (h-fn new-state)))]
    (Node. node new-state (.operation step) new-cost new-value)))

(defn- general-search [state expand-fn h-fn constraint goal-fn the-limit]
  (let [limit ^double the-limit]
    (loop [queue          (:strategy state)
           contour        (Double/POSITIVE_INFINITY)
           visited        (long (get state :visited 0))
           expanded       (long (get state :expanded 0))]
      (when (Thread/interrupted) (throw (InterruptedException.)))
      (if-let [^Node node (s-peek queue)]
        (if (on-visit constraint node)
          (recur (s-pop! queue) contour (inc visited) expanded)
          (let [f-cost (.value node)
                queue  (s-pop! queue)]
;              (when (= (rem visited 10000) 0) (println "..." f-cost))
              (if (goal-fn (.state node))
                {:node node :contour contour :visited visited :expanded expanded
                 :next-solver #(general-search { :strategy queue :visited visited :expanded expanded} expand-fn h-fn constraint goal-fn limit)}
                (let [moves (expand-fn (.state node))
                      [queue expanded contour]
                        (loop [queue    queue
                               contour  contour
                               expanded (long expanded)
                               moves    moves]
                          (if-let [move (first moves)]
                            (let [childnode ^Node (expand-node node h-fn move)]
                              (if (on-expand constraint childnode)
                                (recur queue contour expanded (next moves))
                                (if (> (.value childnode) limit)
                                  (recur queue (double (min contour (.value childnode))) expanded (next moves))
                                  (recur (s-conj! queue childnode) contour (inc expanded) (next moves)))))
                            [queue expanded contour]))]
                  (recur queue (double contour) (inc visited) (long expanded))))))
        {:node nil :contour contour :visited visited :expanded expanded}))))

(defn- general-solver [strategy root-node goal-fn expand-fn h-fn constraint limit]
  (let [state {:strategy (s-conj! strategy root-node)}]
    #(general-search state expand-fn h-fn (constraint) goal-fn limit)))

(defn- df-solver [root-node goal-fn expand-fn h-fn constraint limit]
  (general-solver (create-df-strategy) root-node goal-fn expand-fn h-fn constraint limit))

(defn- IDA* [root-node goal-fn expand-fn h-fn constraint limit]
  (loop [last-result ((df-solver root-node goal-fn expand-fn h-fn constraint 0.0))]
    (if (or (:node last-result) (> ^double (:contour last-result) ^double limit)) ; FIXME doesn't stop when contour is infinity when there is no limit!!
      last-result
      (do (println "limit" (:contour last-result))
          (recur (merge-result last-result ((df-solver root-node goal-fn expand-fn h-fn constraint (:contour last-result)))))))))

(defn- IDA*-solver [root-node goal-fn expand-fn h-fn constraint limit]
  #(IDA* root-node goal-fn expand-fn h-fn constraint limit))

(defn- do-solve [solver]
  (create-public-result (solver)))

(defn- do-solve-all [solver]
  (if solver
    (lazy-seq
      (let [result (solver)]
        (if (:node result)
          (cons (create-public-result result) (do-solve-all (:next-solver result))))))))

(defn solve [root-state goal-fn expand-fn & {:keys [algorithm heuristic limit constraint all]
                                             :or {algorithm  :A*
                                                  heuristic  (constantly 0.0)
                                                  limit      Double/POSITIVE_INFINITY
                                                  constraint (no-constraint)
                                                  all        false}}]
  (let [root-node (Node. nil root-state nil 0.0 (heuristic root-state))
        solver    (cond (= algorithm :A*)   (general-solver (create-A*-strategy) root-node goal-fn expand-fn heuristic constraint limit)
                        (= algorithm :DF)   (general-solver (create-df-strategy) root-node goal-fn expand-fn heuristic constraint limit)
                        (= algorithm :BF)   (general-solver (create-bf-strategy) root-node goal-fn expand-fn heuristic constraint limit)
                        (= algorithm :IDA*) (IDA*-solver                         root-node goal-fn expand-fn heuristic constraint limit)
                             :default            (throw (IllegalArgumentException.
                                                          (str "Unknown algorithm: " algorithm ", supperted are: [:A* :IDA* :DF]"))))
        function  (if all do-solve-all do-solve)]
    (function solver)))

(defn get-operations [result]
  (->> result :solution rest (map :operation)))