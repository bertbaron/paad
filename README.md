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
       :solution
       [{:cost 0.0, :state [5 1 3 2 4], :operation nil}
        {:cost 1.0, :state [1 5 3 2 4], :operation 0}
        {:cost 2.0, :state [1 5 2 3 4], :operation 2}
        {:cost 3.0, :state [1 2 5 3 4], :operation 1}
        {:cost 4.0, :state [1 2 3 5 4], :operation 2}
        {:cost 5.0, :state [1 2 3 4 5], :operation 3}]}

The result contains statistics that show how many nodes have been expanded and how many 
nodes have been visited by the algorithm. This can be quite useful during tuning.

The solution contains the complete path from initial state to goal state, with the applied
operation and total cost to reach the state. This is typically more than needed, but to
extract the required information is quite simple. For example, to just list the operations
(the indices of the left element of the swap in our example), use the following in the repl:
(where *1, the last result, is assumed to contain the result): 

    (map :operation (rest (:solution *1)))

    > (0 2 1 2 3)

### Choosing the algorithm

By default *A*** is used to solve the problem. To use an other algorithm, simply specify it
using the :algorithm key:

    (p/solve [5 1 3 2 4] goal? expand
             :algorithm :BF)
    
    >  {:statistics {:expanded 1776, :visited 444},
       :solution
       [{:cost 0.0, :state [5 1 3 2 4], :operation nil}
        ...
        {:cost 5.0, :state [1 2 3 4 5], :operation 3}]}    

In this case it seems that Breadth-first search results in fewer nodes expanded and visited.
This is because A* will behave similar to Breadth-first in case no heuristic is provided and
the costs for each step is the same.

### Tuning

We now have a program that can solve our problem and this may be all we need. However, if we
expect to solve the problem for larger input vectors we may run into trouble. Even other
vectors of length 5 will already show this:

    (:statistics (p/solve [5 4 3 2 1] goal? expand))  

    > {:expanded 1399388, :visited 349847}
    
That seems like a huge number of nodes for such a small input. The reason is that the algorithm
has exponential performance, and in case of A* also memory usage. Every state expands to 4 child-states.
The number of nodes at depth 10 is therefore 4^10=1048576.

It is almost always necessary to reduce the size of the search tree in order to be able to solve
slightly more complex problems.  

#### Using constraints

The easiest way to reduce the size of the tree is by trying to see if one of the provided constraints
is suitable for the problem.

##### no-return-constraint

Analyzing our problem domain, it is clear that it makes no sense to swap the same elements twice after
each other, since we will return to original state. This is what the '''no-return-constraint''' does,
it cuts the branch if an operation of a state returns in the same state or in the parent state.
 
    (:statistics (p/solve [5 4 3 2 1] goal? expand
                          :constraint (p/no-return-constraint)))

    > {:expanded 118357, :visited 39452}

This saves an order of magnitude, since every expand will now effectively result in 3 child states
instead of four. This means we should now be able to compute vectors with one more element at the
same costs.

The no-loop-constraint has a constant time overhead per node and no memory overhead, so it typically
always pays off to use it if applicable.

##### no-loop-constraint

Not only swapping the same element twice will result in a previous state. Consider the case where
we make the swaps 0, 2, 0, 2. This will also result in a previous state. Loops like these can be cut
of with the no-loop-constraint:

    (:statistics (p/solve [5 4 3 2 1] goal? expand
                          :constraint (p/no-loop-constraint)))

    > {:expanded 78843, :visited 28439}

This constraint is somewhat more costly than the no-return-constraint, and in some cases it barely has
any effect, for example when a good heuristic is provided.

##### cheapest-path-constraint

Finally the library provides a constraint that detects if a state is reached via multiple paths, and
only continues with the best path to that state:

    (:statistics (p/solve [5 4 3 2 1] goal? expand
                          :constraint (p/cheapest-path-constraint)))

    > {:expanded 119, :visited 119}
    
This constraint basically turns the search tree into an actual graph of unique states. It works perfectly
for problems where the number of unique states is limited. The memory consumption is linear in
the number of unique states however.

#### using heuristics



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
