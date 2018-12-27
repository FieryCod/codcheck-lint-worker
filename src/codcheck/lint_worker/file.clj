(ns codcheck.lint-worker.file
  (:require
   [taoensso.timbre :as timbre])
  (:import
   [java.io File]))

(defn create-folder-when-not-exists
  [path]
  (timbre/info "Creating folder structure for" path)
  (.mkdirs (File. path)))

(defn- read-project-configuration-from-projectclj
  [path]
  (apply hash-map (subvec (vec (read-string (slurp path))) 3)))

(defn get-absolute-project-path
  []
  (.getAbsolutePath (File. "")))

(defn get-canonical-path
  [^String path]
  (.getAbsolutePath (File. path)))
