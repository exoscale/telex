(ns exoscale.telex.interceptor.ring1
  (:require [exoscale.interceptor :as ix]
            [exoscale.telex.interceptor :as interceptor]
            [exoscale.telex.request :as request]
            [exoscale.telex.response :as response])
  (:import (java.net.http HttpRequest
                          HttpResponse)))

(defn- ring1->http-request
  ^HttpRequest
  [{:ring/keys [request]
    :exoscale.telex.request/keys [timeout version expect-continue?]}]
  (let [{:keys [url query method body headers] :or {method :get}} request]
    (request/http-request url query method body headers timeout version
                          expect-continue?)))

(defn- http-response->ring1
  [^HttpResponse http-response]
  {:status (response/status http-response)
   :body (response/body http-response)
   :headers (response/headers->map http-response)})

(def ring-format-interceptor
  {:name ::map-format-interceptor
   :enter (fn [ctx]
            ;; we consider all non ns keys, ring-keys
            (let [request-keys (reduce-kv (fn [m k v]
                                            (cond-> m
                                              (not (namespace k))
                                              (assoc k v)))
                                          {}
                                          ctx)]
              (-> (apply dissoc ctx (keys request-keys))
                  (assoc :ring/request request-keys))))
   :leave (fn [{:as ctx :ring/keys [request response]}]
            ;; we just care about the final request output, hide
            ;; original request for potential output, put it in meta
            ;; instead (prevent potential POST secrets leaking in
            ;; logs)
            (-> ctx
                (dissoc :ring/response :ring/request)
                (conj response)
                (vary-meta assoc :ring/request request)))})

(def request-interceptor
  {:name ::request
   :enter (fn [ctx]
            (assoc ctx
                   :exoscale.telex/request (ring1->http-request ctx)))
   :leave (fn [ctx]
            (assoc ctx
                   :ring/response
                   (http-response->ring1 (:exoscale.telex/response ctx))))})

(def query-params-interceptor
  {:name ::query-params
   :enter
   (-> interceptor/encode-query-params
       (ix/in [:ring/request :query-params])
       (ix/out [:ring/request :query]))})

(def form-params-interceptor
  {:name ::form-params
   :enter (-> interceptor/encode-query-params
              (ix/in [:ring/request :form-params])
              (ix/out [:ring/request :body])
              (ix/when (fn [{:ring/keys [request]}]
                         (and (not (:body request))
                              (seq (:form-params request))))))})

(def interceptor-chain
  [(interceptor/throw-on-err-status-interceptor [:status])
   ring-format-interceptor
   query-params-interceptor
   form-params-interceptor
   request-interceptor
   interceptor/send-interceptor])
