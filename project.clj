(defproject exoscale/telex "0.1.4"
  :description "java.net.http helpers"
  :license {:name "ISC"}
  :url "https://github.com/exoscale/telex"
  :deploy-repositories [["releases" :clojars] ["snapshots" :clojars]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [exoscale/interceptor "0.1.9"]
                 [exoscale/ex "0.3.17"]
                 [exoscale/ex-auspex "0.3.17"]
                 [exoscale/ex-http "0.3.17"]
                 [cc.qbits/auspex "0.1.0-alpha4"]
                 [cc.qbits/commons "1.0.0-alpha3"]]
  :profiles {:dev  {:dependencies [[ring/ring-jetty-adapter    "1.7.1"]]}
             :test {:dependencies []}}
  :plugins [[lein-cljfmt "0.7.0"]]
  :cljfmt {:remove-multiple-non-indenting-spaces? true}
  :pedantic? :warn
  :global-vars {*warn-on-reflection* true})
