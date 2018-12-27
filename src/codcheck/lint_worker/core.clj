(ns codcheck.lint-worker.core
  (:require
   [langohr.consumers]
   [codcheck.lint-worker handler]
   [codcheck logging rmq]
   [taoensso.timbre :as timbre])
  (:gen-class))

(defn- setup []
  (codcheck.logging/setup-logging "codcheck-lint-worker")

  (timbre/info "Connecting to RabbitMQ Cluster...")
  (codcheck.rmq/connect!)
  (codcheck.rmq/open-chan!)
  (timbre/info "Connection established" @codcheck.rmq/conn "with chan" @codcheck.rmq/chan))

(defn -main
  [& args]
  (setup)
  (langohr.consumers/subscribe @codcheck.rmq/chan
                               (:pr-code-check codcheck.rmq/queues)
                               codcheck.lint-worker.handler/on-new-pr
                               {:auto-ack true}))
