(defproject paad "0.1.0-SNAPSHOT"
  :description "Clojure library for problem solving with search algorithms like Depth-first, A* and IDA*"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs" "src/brepl"]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2069"]]

  :plugins [[lein-cljsbuild "1.0.0"]]

  ;; cljsbuild tasks configuration
  :cljsbuild {:builds
                [{;; clojurescript source code path
                  :source-paths ["src/cljs"]

                  ;; Google Closure Compiler options
                  :compiler {;; the name of emitted JS script file
                             :output-to "resources/public/js/paad.js"

                             ;; minimum optimization
                             :optimizations :whitespace

                             ;; prettyfying emitted JS
                             :pretty-print true}}]})  
