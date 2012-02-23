(ns memory.core
  (:require [goog.dom :as gdom]
            [goog.dom.classes :as gclasses]
            [goog.events :as gevents]
            [clojure.browser.repl :as repl]))

(defn shuffle
  "Fisher–Yates shuffle"
  [coll]
  (reduce (fn [coll i]
            (let [j (rand-int (count coll))]
              (assoc coll i (nth coll j) j (nth coll i))))
          (vec coll)
          (range (count coll))))

(def *number-of-cards* 8)
(def *number-of-group* 2)

(def cards (atom []))
(def current-selection (atom #{}))

(defn populate-cards!
  "Populate cards atom with a newly shuffled set of groups."
  []
  (reset! cards (vec (shuffle (mapcat (fn [a] (take *number-of-group* (repeat a)))
                                      (range *number-of-cards*))))))

(defn card-element
  "Retrieve gdom element associate with card at pos."
  [pos]
  (aget (gdom/getElementsByTagNameAndClass "div" "card") pos))

(defn show-card!
  "Show card face for pos, when not already visible."
  [pos]
  (let [e (card-element pos)]
    (when-not (gdom/getElementByClass "face" e)
      (doto e
        (gclasses/add "open")
        (gdom/append (doto (gdom/createElement "div")
                       (gclasses/add "face")
                       (gdom/append (str (@cards pos)))))))))

(defn game-over!
  "Render game over message."
  []
  (doto (gdom/getElement "board")
    (gdom/removeChildren)
    (gdom/append (doto (gdom/createElement "h1")
                   (gclasses/add "finished")
                   (gdom/append "GAME OVER")))))

(def worker (atom nil))

(defn process-selection!
  "Process current selection, removing the cards when a group is cleared."
  []
  (reset! worker nil)
  (let [cleared (= 1 (count (into #{} (map @cards @current-selection))))]
    (doseq [pos @current-selection]
      (let [e (card-element pos)]
        (doto e
          (gclasses/remove "open")
          (gclasses/enable "cleared" cleared))
        (gdom/removeChildren e)))
    (when cleared
      (swap! cards (fn [cards]
                     (reduce #(assoc %1 %2 nil) cards @current-selection))))
    (when (empty? (filter identity @cards))
      (game-over!)))
  (reset! current-selection #{}))

(defn handle-card-click!
  "Handle card click for a given pos."
  [pos]
  (when @worker
    (js/clearTimeout @worker)
    (process-selection!))
  (when (and (@cards pos)
             (not (@current-selection pos))
             (< (count @current-selection) *number-of-group*))
    (swap! current-selection conj pos)
    (show-card! pos)
    (when (= (count @current-selection) *number-of-group*)
      (reset! worker (js/setTimeout process-selection! 1000)))))

(defn create-card-element!
  "Create dom element for a card and bind click listener."
  [pos]
  (doto (gdom/createElement "div")
    (gclasses/add "card")
    (gevents/listen "click" (fn [] (handle-card-click! pos)))))

(defn render-cards!
  "Render the cards on the board."
  []
  (let [board (gdom/getElement "board")]
    (gdom/removeChildren board)
    (dotimes [pos (count @cards)]
      (let [elm (create-card-element! pos)]
        (gdom/append board elm)))))

(populate-cards!)
(render-cards!)

(when (re-find #"\?debug" (. window/location -href))
  (repl/connect "http://localhost:9000/repl"))