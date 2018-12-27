(ns codcheck.lint-worker.git
  (:require
   [taoensso.timbre :as timbre]
   [clojure.java.shell :refer [sh]]
   [codcheck rmq auth]))

#_
"
  Git commands always put status of command to stderr.
  Errors starts with 'fatal:'
"

(def ^:private git-protocol
  "git://")

(def ^:private github-err-regexp
  #"fatal: (.*)")

(defmacro ^:private execute-git-command
  [& cmd-strs]
  `(let [command# (sh ~@cmd-strs)
         out# (-> command# :out)
         fatal# (re-find github-err-regexp out#)]
       (if (nil? fatal#)
         (timbre/info "Succesfully completed command:" (clojure.string/join " " [~@cmd-strs]) "with result" out#)
         (throw (Exception. (str "Couldn't lint the project because of: " (second fatal#)))))))

(defn- clone-url
  [event repo-url]
  (let [installation-id (-> event :installation :id)
        gh-signed-token (codcheck.auth/gh-sign-token)
        installation-token (codcheck.auth/installation-request->token
                            (codcheck.auth/gh-installation-token-request installation-id gh-signed-token))
        repo-url-without-protocol (clojure.string/join (drop (count git-protocol) repo-url))]
    (str "https://x-access-token:" installation-token "@" repo-url-without-protocol)))

(defn clone-repo
  [event repo-url repo-identifier repo-clone-path]
  (timbre/info "Cloning application" repo-identifier "to" repo-clone-path)
  (execute-git-command "git" "clone" (clone-url event repo-url) repo-clone-path))


(defn checkout-code
  [path commit-sha]
  (timbre/info "Checkout code to" commit-sha)
  (execute-git-command "git" "checkout" commit-sha :dir path))
