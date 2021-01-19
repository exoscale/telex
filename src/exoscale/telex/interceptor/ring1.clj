(ns exoscale.telex.interceptor.ring1
  (:require [exoscale.interceptor :as ix]
            [exoscale.telex.interceptor :as interceptor]
            [exoscale.telex.request :as request]
            [exoscale.telex.response :as response])
  (:import (java.net.http HttpRequest
                          HttpResponse)))

(defn- ring1->http-request
  ^HttpRequest
  [{:ring1.request/keys [url query method body headers]
    :exoscale.telex.request/keys [timeout version expect-continue?]
    :or {method :get}
    :as ct}]
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
            (->> ctx
                 (reduce-kv (fn [m k v]
                              (assoc! m
                                      (if (namespace k)
                                        k
                                        (keyword "ring1.request" (name k)))
                                      v))
                            (transient {}))
                 (persistent!)))
   :leave (fn [ctx]
            (->> ctx
                 (reduce-kv (fn [m k v]
                              (cond-> m
                                (= (namespace k) "ring1.response")
                                (assoc! (keyword (name k)) v)))
                            (transient ctx))
                 (persistent!)))})

(def request-interceptor
  {:name ::request
   :enter (fn [ctx]
            (assoc ctx :exoscale.telex/request (ring1->http-request ctx)))
   :leave (fn [ctx]
            (into ctx (http-response->ring1 (:exoscale.telex/response ctx))))})

(def query-params-interceptor
  {:name ::query-params
   :enter
   (-> interceptor/encode-query-params
       (ix/in [:ring1.request/query-params])
       (ix/out [:ring1.request/query]))})

(def form-params-interceptor
  {:name ::form-params
   :enter (-> interceptor/encode-query-params
              (ix/in [:ring1.request/form-params])
              (ix/out [:ring1.request/body])
              (ix/when (fn [{:ring1.request/keys [form-params body]}]
                         (and (not body)
                              (seq form-params)))))})

(def interceptor-chain
  [(interceptor/throw-on-err-status-interceptor [:status])
   ring-format-interceptor
   query-params-interceptor
   form-params-interceptor
   request-interceptor
   interceptor/send-interceptor])
