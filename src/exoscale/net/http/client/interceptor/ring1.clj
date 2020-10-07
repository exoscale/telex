(ns exoscale.net.http.client.interceptor.ring1
  (:require [exoscale.interceptor :as ix]
            [exoscale.net.http.client.interceptor :as interceptor]
            [exoscale.net.http.client.request :as request]
            [exoscale.net.http.client.response :as response])
  (:import (java.net.http HttpRequest
                          HttpResponse)))

(defn- ring1->http-request
  ^HttpRequest
  [{:keys [url query method body headers timeout version
           expect-continue?]
    :or {method :get}}]
  (request/http-request url query method body headers timeout version
                        expect-continue?))

(defn- http-response->ring1
  [^HttpResponse http-response]
  {:status (response/status http-response)
   :body (response/body http-response)
   :headers (response/headers->map http-response)})

(def interceptor
  {:name ::ring1
   :enter (fn [{:as ctx :keys [request]}]
            (assoc ctx :exoscale.net.http/request (ring1->http-request request)))
   :leave (fn [{:as ctx :exoscale.net.http/keys [response]}]
            (assoc ctx :response (http-response->ring1 response)))})

(def interceptor-chain
  [{:leave (fn [ctx] (:response ctx))}
   (interceptor/throw-on-err-status-interceptor [:response])
   {:enter (-> interceptor/encode-query-params
               (ix/in [:request :query-params])
               (ix/out [:request :query]))}
   #'interceptor
   #'interceptor/send-interceptor])
