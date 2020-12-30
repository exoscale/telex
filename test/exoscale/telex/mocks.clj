(ns exoscale.telex.mocks
  (:require [ring.adapter.jetty             :refer [run-jetty]]))

(defmacro with-server
  [port handler & body]
  `(let [server# (run-jetty ~handler {:port ~port :join? false})]
     (try
       ~@body
       (finally
         (.stop server#)))))
