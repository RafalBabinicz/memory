(ns memory.core
  (:require [goog.dom :as gdom]
            [goog.dom.classes :as gclasses]
            [goog.events :as gevents]
            [goog.style :as gstyle]
            [clojure.browser.repl :as repl]))

(defn shuffle
  "Fisher–Yates shuffle"
  [coll]
  (reduce (fn [coll i]
            (let [j (rand-int (count coll))]
              (assoc coll i (nth coll j) j (nth coll i))))
          (vec coll)
          (range (count coll))))

(def *number-of-cards* 10)
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
    (gstyle/showElement false)
    gdom/removeChildren)
  (gstyle/showElement (gdom/getElement "cover") true))

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
    (.clearTimeout js/window @worker)
    (process-selection!))
  (when (and (@cards pos)
             (not (@current-selection pos))
             (< (count @current-selection) *number-of-group*))
    (swap! current-selection conj pos)
    (show-card! pos)
    (when (= (count @current-selection) *number-of-group*)
      (reset! worker (.setTimeout js/window process-selection! 1000)))))

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
        (gdom/append board elm)))
    (gstyle/showElement board true)))

(defn- floor
  "Wrap js/Math.floor function."
  [val] (.floor js/Math val))

(defn insert-style!
  "Insert window size specific style."
  []
  (let [border 6
        margin (floor (max (/ window/innerWidth 200)
                           (/ window/innerHeight 200)))
        size (- (first (filter (fn [size]
                                 (< (floor (/ (* 2 *number-of-cards*)
                                              (floor (/ window/innerWidth size))))
                                    (floor (/ window/innerHeight size))))
                               (range 500 10 -10)))
                (* 2 margin)
                border)
        style (gdom/createElement "style")]
    (gdom/append style (str "div.card{margin:" margin "px;width:" size "px;height:" size "px}"
                            "div#cover,div.card div.face{font-size:" (floor (* size 0.8)) "px}"))
    (gdom/append document/body style)))

(defn start-new-game!
  "Start a new game of memory."
  []
  (gstyle/showElement (gdom/getElement "cover") false)
  (populate-cards!)
  (render-cards!))

(insert-style!)
(gstyle/showElement (gdom/getElement "cover") true)
(gevents/listen (gdom/getElement "play-link") "click" start-new-game!)

(when (re-find #"\?debug" (. window/location -href))
  (repl/connect "http://localhost:9000/repl"))