(ns exoscale.telex.mocks
  (:require [ring.adapter.jetty :as jetty]))

(defmacro with-server
  [port handler & body]
  `(let [server# (jetty/run-jetty ~handler {:port ~port :join? false})]
     (try
       ~@body
       (finally
         (.stop server#)))))
