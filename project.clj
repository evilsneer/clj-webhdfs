(defproject emptyone/webhdfs "1.0.0-SNAPSHOT"
  :description "Clojure webhdfs lib using org.httpkit.client"
  :url "https://github.com/evilsneer/clj-webhdfs"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :plugins [[org.apache.maven.wagon/wagon-ssh-external "2.6"]]
  :deploy-repositories ^:replace [["releases" {:url           "scp://root@10.20.30.111/root/rel/"
                                               :sign-releases false}]
                                  ["snapshots" "scp://root@10.20.30.111/root/snap/"]]

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [log4j/log4j "1.2.17"]
                 [org.clojure/tools.logging "0.4.1"]
                 [environ "1.1.0"]
                 [http-kit "2.3.0"]
                 [org.clojure/data.json "0.2.6"]]
  :profiles {:uberjar {:aot :all}}
  :repl-options {:init-ns webhdfs.core})

(cemerick.pomegranate.aether/register-wagon-factory!
  "scp" #(let [c (resolve 'org.apache.maven.wagon.providers.ssh.external.ScpExternalWagon)]
           (clojure.lang.Reflector/invokeConstructor c (into-array []))))