# paad

Clojure library for problem solving with search algorithms like Depth-first, A\* and IDA\*

## Install

[![Clojars Project](https://img.shields.io/clojars/v/bertbaron/paad.svg)](https://clojars.org/bertbaron/paad)

## Usage

As an example we will sort a vector of elements with the minimal number of swaps
of neighboring elements:

```clojure
(ns bertbaron.paad.examples.swaps
  (:require [bertbaron.paad.core :as p]))

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
```
results in:
```clojure
{:statistics {:expanded 2628, :visited 657},
 :solution
 [{:cost 0.0, :state [5 1 3 2 4], :operation nil}
  {:cost 1.0, :state [1 5 3 2 4], :operation 0}
  {:cost 2.0, :state [1 5 2 3 4], :operation 2}
  {:cost 3.0, :state [1 2 5 3 4], :operation 1}
  {:cost 4.0, :state [1 2 3 5 4], :operation 2}
  {:cost 5.0, :state [1 2 3 4 5], :operation 3}]}
```

The expand function returns the child states together with the operation that was applied
and the cost of the step. The operation is not used by the framework but is included in
the result.

The result contains statistics that show how many nodes have been expanded and how many 
nodes have been visited by the algorithm. This can be quite useful during tuning.

The solution contains the complete path from initial state to goal state, with the applied
operation and total cost to reach each state. This is typically more than needed, but to
extract the required information is quite simple. For example, to just list the operations
(the indices of the left element of the swap in our example), use the following in the repl:

```clojure
> (p/get-operations *1)
(0 2 1 2 3)
```

### Choosing the algorithm

#### A*
 
By default *A** is used to solve the problem. It can also explicitly be specified using
the :algorithm key:

```clojure
> (p/get-operations (p/solve [5 1 3 2 4] goal? expand
                             :algorithm :A*))
(0 2 1 2 3)   
```

A* will find the optimal solution in the least number of visited nodes if an admissible
heuristic is provided. The *heuristic function* estimates the remaining costs to reach
the goal for a given state. An admissible heuristic is a heuristic that never over-estimates
the actual costs of the best solution.

Since A* keeps all nodes in memory, it may run out of memory before a solution has been
found

#### IDA*

Iterative Deepening A*. Returns the optimal solution like A*, but uses
very little memory by using a depth first search in iterations with increasing limit. Works very well when the costs increase in discrete steps along the path.

```clojure
> (p/get-operations (p/solve [5 1 3 2 4] goal? expand
                             :algorithm :IDA*))
(2 0 1 2 3)
```
    
#### Depth First

Explores as far as possible along each branch before backtracking. Will not guarantee to
find the optimal solution. Requires very little memory (linear in the deepest path).

A limit can be provided to avoid that the search is expanding into depth forever.

```clojure
> (p/get-operations (p/solve [5 1 3 2 4] goal? expand
                             :algorithm :DF
                             :limit 8))
(3 3 2 0 1 2 3)
```

#### Breadth First

Expands all nodes at a specific depth before going to the next
depth. Will find the optimal solution if the shortest path is the optimal solution.
Requires a lot of memory. It is almost always better to use A* or IDA*.

```clojure
> (p/get-operations (p/solve [5 1 3 2 4] goal? expand
                             :algorithm :BF))
(0 1 2 1 3)
```

### Tuning

We now have a program that can solve our problem and this may be all we need. However, if we
expect to solve the problem for larger input vectors we may run into trouble. Even other
vectors of length 5 will already show this:

```clojure
> (:statistics (p/solve [5 4 3 2 1] goal? expand))  
{:expanded 1399388, :visited 349847}
```
    
That seems like a huge number of nodes for such a small input. The reason is that the algorithm
has exponential complexity, and in case of A* also memory usage. Every state expands to 4 child-states.
The number of nodes at depth 10 is therefore 4^10=1048576.

It is almost always necessary to reduce the size of the search tree in order to be able to solve
slightly more complex problems. 

#### using heuristics

The most obvious and powerful way to reduce the search space when using A\* or IDA\* is of course the *heuristic function*.

For our problem an admissible heuristic could be the distance from each element to its target position,
divided by 2, since each step will only move two elements by one position each:

```clojure
> (defn heuristic [^java.util.List state]
    (let [target ^java.util.List (sort state)
          displacement (reduce + (for [item state]
                                   (Math/abs (- (.indexOf state item)
                                                (.indexOf target item)))))]
      (/ displacement 2)))
#user/heuristic
> (:statistics (p/solve [5 4 3 2 1] goal? expand
                        :heuristic heuristic))
{:expanded 1676, :visited 419} 
```
> NOTE: For simplicity we assume that the state does not contain duplicates. This heuristic will not be
> admissible if it does!


#### Using constraints

The easiest way to reduce the size of the tree is by trying to see if one of the provided constraints
is suitable for the problem.

##### no-return-constraint

Analyzing our problem domain, it is clear that it makes no sense to swap the same elements twice after
each other, since we will return to original state. This is what the '''no-return-constraint''' does,
it cuts the branch if an operation of a state returns in the same state or in the parent state.

```clojure
> (:statistics (p/solve [5 4 3 2 1] goal? expand
                          :constraint (p/no-return-constraint)))
{:expanded 118357, :visited 39452}
```

This saves an order of magnitude, since every expand will now effectively result in 3 child states
instead of four. This means we should now be able to compute vectors with one more element at the
same costs.

The no-loop-constraint has a constant time overhead per node and no memory overhead, so it typically
always pays off to use it if applicable.

##### no-loop-constraint

Not only swapping the same element twice will result in a previous state. Consider the case where
we make the swaps (0 2 0 2). This will also result in a previous state. Loops like these can be cut
of with the no-loop-constraint:

```clojure
> (:statistics (p/solve [5 4 3 2 1] goal? expand
                        :constraint (p/no-loop-constraint)))
{:expanded 78843, :visited 28439}
```

This constraint is somewhat more costly than the no-return-constraint, and in some cases it barely has
any effect, for example when a good heuristic is provided.

##### cheapest-path-constraint

Finally the library provides a constraint that detects if a state is reached via multiple paths, and
only continues with the best path to that state. This will for example detect that (0 2) and (2 0)
lead to the same state and continue with only one of them:

```clojure
> (:statistics (p/solve [5 4 3 2 1] goal? expand
                        :constraint (p/cheapest-path-constraint)))
{:expanded 119, :visited 119}
```
    
This constraint basically turns the search tree into an actual graph of unique states. It works perfectly
for problems where the number of unique states is limited. The memory consumption is linear in
the number of unique states however.

By default, equals is used for comparing states. It is also possible to provide a key function that
calculates the key from the state.

### Finding all solutions

It is also possible to find all solutions for a problem, by specifying ```:all true```. Instead
of a single result, a lazy sequence of the result is returned:

```clojure
> (map p/get-operations (take 5 (p/solve [5 1 3 2 4] goal? expand
                                         :algorithm :A*
                                         :all true)))
((0 2 1 2 3) (0 1 2 3 1) (2 0 1 2 3) (0 1 2 1 3) (2 0 0 0 1 2 3))
```

The sequence is really lazy, the head will not be calculated until requested
and chunking is not supported. 

This option is not supported for IDA*; if used in combination with
IDA* the sequence will contain not more than one result.

Note that any constraint is respected.

## License

Copyright © 2017

Distributed under the Eclipse Public License version 1.0
