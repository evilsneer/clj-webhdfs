(defproject emptyone/webhdfs "1.0.10"
  :description "Clojure webhdfs lib using org.httpkit.client"
  :url "https://github.com/evilsneer/clj-webhdfs"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :plugins [[org.apache.maven.wagon/wagon-ssh-external "3.5.1"]]
  ;:deploy-repositories ^:replace [["releases" {:url           "scp://root@10.20.30.111/root/rel/"
  ;                                             :sign-releases false}]
  ;                                ["snapshots" "scp://root@10.20.30.111/root/snap/"]]

  ;:deploy-repositories [["releases" {:sign-releases false :url "https://clojars.org/repo"}]
  ;                      ["snapshots" {:sign-releases false :url "https://clojars.org/repo"}]]

  :release-tasks ^:replace [["vcs" "assert-committed"]
                            ["change" "version" "leiningen.release/bump-version" "release"]
                            ["vcs" "commit"]
                            ["vcs" "tag" "--no-sign"]
                            ["deploy" "nexus-m9"]
                            ["change" "version" "leiningen.release/bump-version"]
                            ["vcs" "commit"]
                            ["vcs" "push"]]

  :dependencies [[org.clojure/clojure "1.10.3"]
                 [log4j/log4j "1.2.17"]
                 [org.clojure/tools.logging "1.2.4"]
                 [environ "1.2.0"]
                 [clj-http "3.12.3"]
                 [org.clojure/data.json "2.4.0"]
                 [digest "1.4.10"]]
  :profiles {:uberjar {:aot :all}}
  :aliases {"upg" ["ancient" "upgrade" ":interactive" ":no-tests" ":check-clojure"]}
  :repl-options {:init-ns webhdfs.core})

(cemerick.pomegranate.aether/register-wagon-factory!
  "scp" #(let [c (resolve 'org.apache.maven.wagon.providers.ssh.external.ScpExternalWagon)]
           (clojure.lang.Reflector/invokeConstructor c (into-array []))))