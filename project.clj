(defproject memory "1.0.0-SNAPSHOT"
  :description "A simple game of memory for in a web browser."

  :dependencies [[org.clojure/clojure "1.3.0"]]

  :plugins [[lein-cljsbuild "0.0.14"]]
  
  :cljsbuild {:source-path "src"
              :compiler {:output-to "public/js/memory.js"
                         :output-dir "public/js/memory"}})