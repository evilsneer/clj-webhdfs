(ns webhdfs.core
  (:require
    [clojure.java.io :refer [make-parents delete-file]]
    [clojure.tools.logging :as log]
    [environ.core :refer [env]]
    [clojure.java.shell :refer [sh]]
    [clojure.data.json :as json]
    [clj-http.client :as chc])
  (:gen-class)
  (:import (java.rmi RemoteException)))


(defn run-assertions []
  (doseq [e [:webhdfs #_:tmp-folder]]
    (assert (env e) (format "Set %s in envs!" e))))

(defn- sh+log [& cs]
  (log/info "SH>" cs)
  (log/info (apply sh cs)))

(def username (env :webhdfs-user))

(def webhdfs-v1 (atom (str (env :webhdfs) "/webhdfs/v1")))

#_(defn request
    ([f path params]
     (request f path params 200))
    ([f path params wait-for-code]
     (let [resp @(f (str @webhdfs-v1 path) {:query-params params})]
       (if (= wait-for-code (:status resp))
         (or
           (-> resp :body (json/read-str :key-fn keyword) :FileStatuses :FileStatus)
           true)
         (throw
           (or
             (:error resp)
             (-> resp
               :body
               RemoteException.)))))))

(defn request2
  ([f path params]
   (request2 f path params 200))
  ([f path params wait-for-code]
   (let [resp (f (str @webhdfs-v1 path) {:query-params (merge
                                                         params
                                                         (if username {:user.name username}))})]
     (if (= wait-for-code (:status resp))
       (or
         (-> resp
           :body
           (json/read-str :key-fn keyword)
           :FileStatuses
           :FileStatus)
         true)
       (throw (or (:error resp) (-> resp
                                  :body
                                  RemoteException.)))))))

(defn ls [path]
  (request2 chc/get path {:op "LISTSTATUS"}))

(defn short-ls [path]
  (->>
    (ls path)
    (map :pathSuffix)))

(defn mkdirs [path]
  (request2 chc/put path {:op "MKDIRS"}))

(defn create [path data]
  (let [resp (chc/put
               (str @webhdfs-v1 path)
               {:follow-redirects false
                :query-params     (merge
                                    {:op        "CREATE"
                                     :overwrite "true"}
                                    (if username {:user.name username}))})
        uri  (-> resp
               :headers
               :location)]
    (chc/put
      uri
      {:body data})))

(defn open
  ([path]
   (->
     (str @webhdfs-v1 path)
     (chc/get
       {:query-params {:op "OPEN"}
        :as           :stream})
     :body)))

(defn delete [path]
  (request2 chc/delete path {:op        "DELETE"
                             :recursive "true"}))


(run-assertions)