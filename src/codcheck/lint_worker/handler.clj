(ns codcheck.lint-worker.handler
  (:require
   [langohr basic consumers channel]
   [tentacles.core :as tentacles]
   [clojure.java.shell :refer [sh with-sh-dir]]
   [taoensso.timbre :as timbre]
   [codcheck.lint-worker.linters.linter :as linter]
   [codcheck.lint-worker.linters.kibit :as kibit]
   [codcheck.envs :refer [envs]]
   [codcheck.lint-worker git file]))

(defn- get-single-event
  []
  (langohr.basic/get @codcheck.rmq/chan (:gh-pr-code-check codcheck.rmq/queues) false))

(defn- dev-with-nack-and-requeue
  [processing-fn]
  (let [[{:keys [delivery-tag] :as metadata} ^bytes payload] (get-single-event)]

    ;; Process the payload
    (processing-fn payload)

    ;; Nack and requeue
    (langohr.basic/nack @codcheck.rmq/chan delivery-tag false true)))

(defn- notify-with-result
  [event payload]
  (timbre/info "Lint result body:" payload)
  (langohr.basic/publish @codcheck.rmq/chan
                         (:pr-code-checked codcheck.rmq/exchanges)
                         (:pr-code-checked codcheck.rmq/routing-keys)
                         (str (assoc event :lint-result payload))))

(defmacro propagate-on-error
  [body]
  `(try ~body
        (catch Exception err#
          (.printStackTrace err#)
          (timbre/info "Removing worker..."))))

#_"
 #1 Create a folder for repo if not exists
 #2 Clone the repository
 #3 Checkout codebase
 #4 Do linting process
 #5 Results of linting should be passed to queue
"
;; (codcheck.rmq/connect!)
;; (codcheck.rmq/open-chan!)
(defn on-new-pr
  [_ _ ^bytes payload]
  (propagate-on-error
   (let [event (read-string (slurp payload))
         _ (timbre/info "New PR ready to be processed" "payload: " (pr-str event))
         commit-sha (-> event :pull_request :head :sha)
         username (-> event :pull_request :head :user :login)
         repo-name (-> event :pull_request :head :repo :name)
         repo-url (-> event :pull_request :head :repo :git_url)
         repo-identifier (clojure.string/join "/" [username repo-name commit-sha])
         repo-clone-path (str (:PR_STORE_PATH envs) "/" repo-identifier)
         project-path (codcheck.lint-worker.file/get-absolute-project-path)

         Kibit (kibit/->Kibit)]

     ;; #1
     (timbre/info "Repo clone path:" repo-clone-path)
     (codcheck.lint-worker.file/create-folder-when-not-exists repo-clone-path)

     ;; #2
     (codcheck.lint-worker.git/clone-repo event repo-url repo-identifier repo-clone-path)

     ;; #3
     (timbre/info "Checkouting to" commit-sha "in" repo-clone-path)
     (codcheck.lint-worker.git/checkout-code repo-clone-path commit-sha)

     ;; #4 #5
     (timbre/info "Linting code with Kibit")
     (-> (.lint Kibit repo-clone-path)
         (linter/record->map)
         ((partial notify-with-result event)))
     (timbre/info "Linting process succesfully finished.. Notification event was produced"))))
