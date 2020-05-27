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
    (assert (env e) (format "Set %s in envs!" e))))

(defn- sh+log [& cs]
  (log/info "SH>" cs)
  (log/info (apply sh cs)))

(def username (env :webhdfs-user))

(def webhdfs-env (env :webhdfs))

(defn format- [url]
  (str url "/webhdfs/v1"))

(def a-webhdfs-v1s (let [urls (str/split webhdfs-env #",")]
                     (atom (map format- urls))))

(defn ^:deprecated request
  ([f path params]
   (request f path params 200))
  ([f path params wait-for-code]
   (let [resp @(f (str (first @a-webhdfs-v1s) path) {:query-params params})]
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
   (let [request-params {:query-params
                         (merge
                           params
                           (if username {:user.name username}))}
         resp-fn        #(f % request-params)]
     (loop [webhdfs-urls @a-webhdfs-v1s]
       (let [request-url (str (first webhdfs-urls) path)
             success?    (atom true)
             response    (atom nil)]
         (slingshot/try+
           (let [resp (resp-fn request-url)]
             (if (not= wait-for-code (:status resp))
               (throw (or
                        (:error resp)
                        (-> resp
                          :body
                          RemoteException.)))
               (reset! response (or
                                  (-> resp
                                    :body
                                    (json/read-str :key-fn keyword)
                                    :FileStatuses
                                    :FileStatus)
                                  true))))
           (catch [:status 403] e
             (if (> (count webhdfs-urls) 1)
               (do
                 (log/error (format "catched 403 with %s trying next uri" request-url))
                 (reset! success? false))
               (do
                 (log/error (format "catched 403 with %s no more urls" request-url))
                 (slingshot/throw+ e)))))
         (if @success?
           @response
           (recur (drop 1 webhdfs-urls))))))
   ))

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
               (str (first @a-webhdfs-v1s) path)
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
     (str (first @a-webhdfs-v1s) path)
     (chc/get
       {:query-params (merge
                        {:op "OPEN"}
                        (if username {:user.name username}))
        :as           :stream})
     :body)))

(defn delete [path]
  (request2 chc/delete path {:op        "DELETE"
                             :recursive "true"}))


(run-assertions)