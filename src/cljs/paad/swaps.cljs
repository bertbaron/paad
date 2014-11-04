(ns paad.swaps
  (:require [domina :as dom]
            [domina.events :as ev]))

(defn solve []
  (let [input (dom/value (dom/by-id "input"))]
    (dom/set-value! (dom/by-id "result") (str (seq input)))))

(defn ^:export init []
  (when (and js/document
             (.-getElementById js/document))
    (ev/listen! (dom/by-id "solve") :click solve)))
