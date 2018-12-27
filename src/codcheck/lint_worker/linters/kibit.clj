(ns codcheck.lint-worker.linters.kibit
  (:require
   [clojure.java.shell :refer [sh with-sh-dir]]
   [clojure.string :as string]
   [codcheck.lint-worker.linters.linter :as Linter]
   [taoensso.timbre :as timbre]))

(def kibit-msg-regex #"At ([a-zA-z\._\/]+):([0-9]+):\n(.*\n.*\n.*\n.*\n.*\n.*\n.*\n.*)")

(defn- decorate-group
  [group]
  (let [with-ticks #(str "```\n" % "\n```")
        propositions [(with-ticks (nth group 3)) (with-ticks (nth group 5))]]

    (conj (vec (take 3 group))
          (first propositions)
          (nth group 4)
          (second propositions))))

(defn- kibit-output->messages
  [^String message]
  (let [part-messages (partition 6 (vec (string/split (str "\n" message) #"\n")))
        join-group #(string/join "\n" (rest %))
        decorated-messages (map #(-> % decorate-group join-group) part-messages)]
    decorated-messages))

(defn message->message-params
  [^String s]
  (rest (re-matches kibit-msg-regex s)))

(defrecord Kibit []
  Linter/ILinter

  (lint [this path]
    (timbre/info "Kibit: linting project at path" path)
    (let [kibit-output (:out (with-sh-dir path
                               (sh "lein" "kibit")))
          messages (map #(->> %
                              (message->message-params)
                              (apply Linter/->LintMessage))
                        (kibit-output->messages kibit-output))]
      (Linter/->LintResult "kibit" {} messages))))
