(ns exoscale.net.http.client.interceptor.ring1
  (:require [exoscale.interceptor :as ix]
            [exoscale.net.http.client.interceptor :as interceptor]
            [exoscale.net.http.client.request :as request]
            [exoscale.net.http.client.response :as response])
  (:import (java.net.http HttpRequest
                          HttpResponse)))

(defn- ring1->http-request
  ^HttpRequest
  [{:ring1.request/keys [url query method body headers timeout version
                         expect-continue?]
    :or {method :get}}]
  (request/http-request url query method body headers timeout version
                        expect-continue?))

(defn- http-response->ring1
  [^HttpResponse http-response]
  {:ring1.response/status (response/status http-response)
   :ring1.response/body (response/body http-response)
   :ring1.response/headers (response/headers->map http-response)})

(def ring-format-interceptor
  {:name ::map-format-interceptor
   :enter (fn [ctx]
            (reduce-kv (fn [m k v]
                         (assoc m
                                (if (namespace k)
                                  k
                                  (keyword "ring1.request" (name k)))
                                v))
                       {}
                       ctx))
   :leave (fn [ctx]
            (reduce-kv (fn [m k v]
                         (cond-> m
                           (= (namespace k) "ring1.response")
                           (assoc (keyword (name k)) v)))
                       ctx
                       ctx))})

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
       (ix/in [:ring1.request/query-params])
       (ix/out [:ring1.request/query]))})

(def form-params
  {:name ::form-params
   :enter (-> interceptor/encode-query-params
              (ix/in [:ring1.request/form-params])
              (ix/out [:ring1.request/body]))})

(def interceptor-chain
  [ring-format-interceptor
   (interceptor/throw-on-err-status-interceptor [:ring1.response/status])
   query-params
   form-params
   #'interceptor
   #'interceptor/send-interceptor])
