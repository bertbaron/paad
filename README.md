# paad

Clojure library for problem solving with search algorithms like Depth-first, A* and IDA

## Usage

As an example we will sort a vector of elements with the minimal number of swaps
of neighbouring elements:

    (ns paad.examples.swaps
      (:require [paad.core :as p]))

    (defn swap
      "Swaps element at index i in vector v with its right neighbour"
      [v i]
      (assoc v i (v (inc i)) (inc i) (v i)))

    (defn expand [state]
      (for [i (range 0 (dec (count state)))]
        (p/step i (swap state i) 1.0)))

    (defn goal? [state]
      (= state (sort state)))

    (p/solve [5 1 3 2 4] goal? expand) 

    > {:statistics {:expanded 2628, :visited 657},
       :solution [
           {:cost 0.0, :state [5 1 3 2 4], :operation nil}
           {:cost 1.0, :state [1 5 3 2 4] , :operation 0}
           {:cost 2.0, :state [1 5 2 3 4], :operation 2}
           {:cost 3.0, :state [1 2 5 3 4], :operation 1}
           {:cost 4.0, :state [1 2 3 5 4], :operation 2}
           {:cost 5.0, :state [1 2 3 4 5], :operation 3}]}

The following will return the sequence of operations (indices to swap) in the repl:

    (map :operation (rest (:solution *1)))

    > (0 2 1 2 3)
    
## Algorithms

The following algorithms are provided by the library:

* **Depth first** explores as far as possible along each branch before backtracking.
  Will not guarantee to find the optimal solution. 
  Requires very little memory (linear in the deepest path). 
* **A*** 
* **IDA*** Iterative Deepening A*. Returns the optimal solution like A*, but uses
  very little memory by using a depth first search in iterations. Works very well
  when the costs increase in discrete steps along the path.
* **Breadth first** Expands all nodes at a specific depth before going to the next
  depth. Will find the optimal solution if the shortest path is the optimal solution.
  Requires a lot of memory. It is almost always better to use A* or IDA*.
    

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License version 1.0
