(ns exoscale.telex
  (:require [exoscale.interceptor :as ix]
            [exoscale.telex.interceptor.ring1 :as ring1]
            [exoscale.telex.interceptor.ring2 :as ring2]
            [exoscale.telex.option :as option]
            [qbits.auspex.executor :as exe])
  (:import (java.net.http HttpClient)))

(defn build [{:as options}]
  (let [b (HttpClient/newBuilder)]
    (option/set-options! b options)
    (.build b)))

(def default-client-opts
  #:exoscale.telex{:follow-redirects :normal
                   :version :http-2})

(def default-request-opts
  (merge #:exoscale.telex.request{:async? true
                                  :throw-on-error? true}
         #:exoscale.telex.response{:executor (exe/work-stealing-executor)}))

(defn client
  [opts]
  (build (into default-client-opts opts)))

(defn- make-request-handler
  [chain]
  (fn [client ctx]
    [client ctx]
    (ix/execute (assoc (into default-request-opts ctx)
                       :exoscale.telex/client client)
                (:exoscale.telex.request/interceptor-chain ctx chain))))

(def request (make-request-handler ring1/interceptor-chain))
(def request2 (make-request-handler ring2/interceptor-chain))
