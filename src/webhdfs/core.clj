(ns webhdfs.core
  (:require
    [clojure.java.io :refer [make-parents delete-file]]
    [clojure.tools.logging :as log]
    [org.httpkit.client :as httpcl]
    [environ.core :refer [env]]
    [clojure.java.shell :refer [sh]]
    [clojure.data.json :as json])
  (:gen-class)
  (:import (java.util UUID)
           (java.rmi RemoteException)))


(defn run-assertions []
  (doseq [e [:webhdfs #_:tmp-folder]]
    (assert (env e) (format "Set %s in envs!" e))))

(defn- sh+log [& cs]
  (log/info "SH>" cs)
  (log/info (apply sh cs)))

(def webhdfs-v1 (atom (str (env :webhdfs) "/webhdfs/v1")))

(defn request
  ([f path params]
   (request f path params 200))
  ([f path params wait-for-code]
   (let [resp @(f (str @webhdfs-v1 path) {:query-params params})]
     (if (= wait-for-code (:status resp))
       (or
         (-> resp :body (json/read-str :key-fn keyword) :FileStatuses :FileStatus)
         true)
       (throw (or (:error resp) (-> resp :body #_(json/read-str :key-fn keyword) RemoteException.)))))))

(defn ls [path]
  (request httpcl/get path {:op "LISTSTATUS"}))

(defn short-ls [path]
  (->>
    (ls path)
    (map :pathSuffix)))

(defn mkdirs [path]
  (request httpcl/put path {:op "MKDIRS"}))

(defn create [path data]
  (let [resp @(httpcl/put
                (str @webhdfs-v1 path)
                {:follow-redirects false
                 :query-params     {:op        "CREATE"
                                    :overwrite "true"}})
        uri  (-> resp
               :headers
               :location)]
    @(httpcl/put
       uri
       {:body data})))

(defn open
  ([path & opts]
   (let [file (@(httpcl/get
                  (str @webhdfs-v1 path)
                  {:query-params {:op "OPEN"}})
                :body)]
     (apply slurp file opts))))

(defn delete [path]
  (request httpcl/delete path {:op        "DELETE"
                               :recursive "true"}))


(run-assertions)