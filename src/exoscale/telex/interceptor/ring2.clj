(ns exoscale.telex.interceptor.ring2
  (:require [exoscale.interceptor :as ix]
            [exoscale.telex.interceptor :as interceptor]
            [exoscale.telex.request :as request]
            [exoscale.telex.response :as response])
  (:import (java.net.http HttpRequest
                          HttpResponse)))

(defn- ring2->http-request
  ^HttpRequest
  [{:ring.request/keys [url query method body headers]
    :exoscale.telex.request/keys [timeout version expect-continue?]
    :or {method :get}}]
  (request/http-request url query method body headers timeout version
                        expect-continue?))

(defn- http-response->ring2
  [^HttpResponse http-response]
  #:ring.response{:status (response/status http-response)
                  :body (response/body http-response)
                  :headers (response/headers->map http-response)})

(def request-interceptor
  {:name ::request
   :enter (fn [ctx]
            (assoc ctx :exoscale.telex/request (ring2->http-request ctx)))
   :leave (fn [ctx]
            (into ctx (http-response->ring2 (:exoscale.telex/response ctx))))})

(def form-params-interceptor
  {:name ::form-params
   :enter (-> interceptor/encode-query-params
              (ix/in [:ring.request/form-params])
              (ix/out [:ring.request/body]))})

(def query-params-interceptor
  {:name ::query-params
   :enter
   (-> interceptor/encode-query-params
       (ix/in [:ring.request/query-params])
       (ix/out [:ring.request/query]))})

(def interceptor-chain
  [(interceptor/throw-on-err-status-interceptor [:ring.response/status])
   query-params-interceptor
   form-params-interceptor
   request-interceptor
   interceptor/send-interceptor])
