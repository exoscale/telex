(ns exoscale.net.http.client.interceptor.ring2
  (:require [exoscale.interceptor :as ix]
            [exoscale.net.http.client.interceptor :as interceptor]
            [exoscale.net.http.client.request :as request]
            [exoscale.net.http.client.response :as response])
  (:import (java.net.http HttpRequest
                          HttpResponse)))

(defn- ring2->http-request
  ^HttpRequest
  [{:ring.request/keys [url query method body headers timeout version
                        expect-continue?]
    :or {method :get}}]
  (request/http-request url query method body headers timeout version
                        expect-continue?))

(defn- http-response->ring2
  [^HttpResponse http-response]
  #:ring.response{:status (response/status http-response)
                  :body (response/body http-response)
                  :headers (response/headers->map http-response)})

(def interceptor
  {:name ::ring2
   :enter (fn [ctx]
            (assoc ctx :exoscale.net.http/request (ring2->http-request ctx)))
   :leave (fn [ctx]
            (into ctx (http-response->ring2 ctx)))})

(def interceptor-chain
  [{:leave (fn [ctx] (:response ctx))}
   (interceptor/throw-on-err-status-interceptor [:response])
   {:enter (-> interceptor/encode-query-params
               (ix/in [:request :query-params])
               (ix/out [:request :query]))}
   #'interceptor
   #'interceptor/send-interceptor])
