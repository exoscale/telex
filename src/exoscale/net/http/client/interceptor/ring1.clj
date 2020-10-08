(ns exoscale.net.http.client.interceptor.ring1
  (:require [exoscale.interceptor :as ix]
            [exoscale.net.http.client.interceptor :as interceptor]
            [exoscale.net.http.client.request :as request]
            [exoscale.net.http.client.response :as response])
  (:import (java.net.http HttpRequest
                          HttpResponse)))

(defn- ring1->http-request
  ^HttpRequest
  [{:ring.request/keys [url query method body headers timeout version
                        expect-continue?]
    :or {method :get}}]
  (request/http-request url query method body headers timeout version
                        expect-continue?))

(defn- http-response->ring1
  [^HttpResponse http-response]
  {:ring.response/status (response/status http-response)
   :ring.response/body (response/body http-response)
   :ring.response/headers (response/headers->map http-response)})

(def interceptor
  {:name ::ring1
   :enter (fn [ctx]
            (assoc ctx :exoscale.net.http/request (ring1->http-request ctx)))
   :leave (fn [ctx]
            (into ctx (http-response->ring1 (:exoscale.net.http/response ctx))))})

(def query-params
  {:name ::query-params
   :enter
   (-> interceptor/encode-query-params
       (ix/in [:ring.request/query-params])
       (ix/out [:ring.request/query]))})

(def form-params
  {:name ::form-params
   :enter (-> interceptor/encode-query-params
              (ix/in [:ring.request/form-params])
              (ix/out [:ring.request/body]))})

(def interceptor-chain
  [{:leave (fn [ctx]
             (reduce-kv (fn [m k v]
                          (cond-> m
                            (= (namespace k) "ring.response")
                            (assoc (keyword (name k)) v)))
                        ctx
                        ctx))}
   (interceptor/throw-on-err-status-interceptor [:ring.response/status])
   query-params
   form-params
   #'interceptor
   #'interceptor/send-interceptor])
