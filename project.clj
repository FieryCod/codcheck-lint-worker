(defproject codcheck.lint-worker "0.0.1"
  :description "Codcheck worker for linting Clojure projects"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [codcheck "0.0.3"]
                 [jonase/eastwood "0.3.3"]
                 [org.clojure/core.async "0.4.474"]
                 [com.novemberain/langohr "5.0.0"]]

  :main codcheck.lint-worker.core

  :source-paths ["src"]

  :plugins [[lein-ring "0.12.4"]
            [lein-bikeshed "0.5.1"]
            [lein-kibit "0.1.6"]
            [lein-shell "0.5.0"]]

  :codcheck {:eastwood {}
             :kibit {}
             :bikeshed {}}

  :profiles {:uberjar {:aot :all}}

  :aliases {"ci-check" ["do" ["kibit"] ["eastwood"] ["bikeshed"]]})
