(ns webhdfs.core
  (:require
    [clojure.java.io :refer [make-parents delete-file]]
    [clojure.tools.logging :as log]
    [environ.core :refer [env]]
    [clojure.java.shell :refer [sh]]
    [clojure.data.json :as json]
    [clj-http.client :as chc]
    [slingshot.slingshot :as slingshot]
    [clojure.string :as str])
  (:gen-class)
  (:import (java.rmi RemoteException)))


(defn run-assertions []
  (doseq [e [:webhdfs #_:tmp-folder]]
    (assert (env e) (format "WEBHDFS lib: Set %s in envs!" e))))

(defn- sh+log [& cs]
  (log/info "SH>" cs)
  (log/info (apply sh cs)))

(defn format- [url]
  (str url "/webhdfs/v1"))


(def ^:dynamic username (env :webhdfs-user))

(def ^:dynamic webhdfs-env (env :webhdfs))

(def ^:dynamic webhdfs-url-list
  (let [urls (str/split webhdfs-env #",")]
    (map format- urls)))


(def last-namenode:atom (atom (first webhdfs-url-list)))


(defn call-request-fn [request-fn success-code]
  (let [resp   (request-fn)
        status (:status resp)]
    (if (not= success-code status)
      (throw (or
               (:error resp)
               (-> resp
                 :body
                 RemoteException.)))
      (let [response-body (-> resp
                            :body
                            (json/read-str :key-fn keyword))
            file-status   (-> response-body
                            :FileStatuses
                            :FileStatus)]
        (if-not file-status
          (do
            (log/info "No :FileStatus in response, returning true" {:response response-body
                                                                    :status   status})
            true)
          file-status)))))


(defn request
  ([method-fn path params]
   (request method-fn path params 200))
  ([method-fn path params success-code]
   (let [request-params {:query-params
                         (merge
                           params
                           (if username {:user.name username}))}]
     (loop [webhdfs-urls webhdfs-url-list]
       (let [request-url (str (first webhdfs-urls) path)
             response    (slingshot/try+
                           (let [response (call-request-fn #(method-fn request-url request-params) success-code)]
                             (swap! last-namenode:atom (constantly request-url))
                             response)
                           (catch [:status 403] e
                             (if (> (count webhdfs-urls) 1)
                               (do
                                 (log/error (format "catched 403 with %s trying next uri" request-url))
                                 ::restart)
                               (do
                                 (log/error (format "catched 403 with %s no more urls" request-url))
                                 (slingshot/throw+ e)))))]
         (if-not (= response ::restart)
           response
           (recur (drop 1 webhdfs-urls))))))))


(defn ls [path]
  (request chc/get path {:op "LISTSTATUS"}))

(defn short-ls [path]
  (->>
    (ls path)
    (map :pathSuffix)))

(defn mkdirs [path]
  (request chc/put path {:op "MKDIRS"}))

(defn create [path data]
  (let [resp (chc/put
               (str (first @webhdfs-url-list) path)
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
     (str (first @webhdfs-url-list) path)
     (chc/get
       {:query-params (merge
                        {:op "OPEN"}
                        (if username {:user.name username}))
        :as           :stream})
     :body)))

(defn delete [path]
  (request chc/delete path {:op         "DELETE"
                             :recursive "true"}))


(run-assertions)