(defproject exoscale/net-http "0.1.0-SNAPSHOT"
  :description "java.net.http helpers"
  :license {:name "ISC"}
  :url "https://github.com/exoscale/net-http"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [exoscale/interceptor "0.1.9"]
                 [exoscale/ex "0.3.15"]
                 [exoscale/ex-auspex "0.3.15"]
                 [exoscale/ex-http "0.3.15"]
                 [cc.qbits/auspex "0.1.0-alpha2"]
                 [cc.qbits/commons "0.5.1"]]
  :profiles {:dev  {:dependencies [[manifold "0.1.8"]
                                   [org.clojure/core.async "1.3.610"]]}
             :test {:dependencies []}}
  :pedantic? :warn
  :global-vars {*warn-on-reflection* true})
