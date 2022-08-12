(defproject exoscale/telex "0.1.7-SNAPSHOT"
  :description "java.net.http helpers"
  :license {:name "ISC"}
  :url "https://github.com/exoscale/telex"
  :deploy-repositories [["releases" :clojars] ["snapshots" :clojars]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [exoscale/interceptor "0.1.12"]
                 [exoscale/ex "0.4.0"]
                 [exoscale/ex-auspex "0.4.0"]
                 [exoscale/ex-http "0.4.0"]
                 [cc.qbits/auspex "1.0.0-alpha10"]
                 [cc.qbits/commons "1.0.0-alpha5"]
                 [com.github.mizosoft.methanol/methanol "1.7.0"]]
  :profiles {:dev {:dependencies [[ring/ring-jetty-adapter "1.7.1"]]}
             :test {:dependencies []}}
  :plugins [[lein-cljfmt "0.7.0"]]
  :cljfmt {:remove-multiple-non-indenting-spaces? true}
  :pedantic? :warn
  :global-vars {*warn-on-reflection* true})
