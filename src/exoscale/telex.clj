(ns exoscale.telex
  (:require [exoscale.interceptor :as ix]
            [exoscale.telex.request :as request]
            [exoscale.telex.interceptor.ring1 :as ring1]
            [exoscale.telex.interceptor.ring2 :as ring2]
            [exoscale.telex.client :as client]))

(defn client
  [opts]
  (client/build (into client/default-options opts)))

(defn- make-request-handler
  [chain]
  (fn [client ctx]
    [client ctx]
    (ix/execute (assoc (into request/default-options ctx)
                       :exoscale.telex/client client)
                (:exoscale.telex.request/interceptor-chain ctx chain))))

(def request (make-request-handler ring1/interceptor-chain))
(def request2 (make-request-handler ring2/interceptor-chain))
