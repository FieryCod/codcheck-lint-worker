(ns codcheck.lint-worker.linters.linter)

(defprotocol ILinter
  (^LintResult lint [this ^String path] "Takes the path and lints the project within the path"))

(defrecord LintMessage [^String path
                        ^String line
                        ^String explanation])

(defrecord LintResult [^String linter-type
                       ^clojure.lang.PersistentHashMap metadata
                       ^LintMessage messages])

(defn record->map
  [r]
  (clojure.walk/postwalk
   #(if (record? %)
      (into {} %)
      %)
   r))
