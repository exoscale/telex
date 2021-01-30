(ns exoscale.telex.interceptor.ring1
  (:require [exoscale.interceptor :as ix]
            [exoscale.telex.interceptor :as interceptor]
            [exoscale.telex.request :as request]
            [exoscale.telex.response :as response])
  (:import (java.net.http HttpRequest
                          HttpResponse)))

(def ring-keys [:url :body :method :query :headers :url
                ;; exts
                :form-params :query-params])

(defn- ring1->http-request
  ^HttpRequest
  [{{:keys [url query method body headers] :or {method :get}} :ring/request
    :exoscale.telex.request/keys [timeout version expect-continue?]}]
  (request/http-request url query method body headers timeout version
                        expect-continue?))

(defn- http-response->ring1
  [^HttpResponse http-response]
  {:status (response/status http-response)
   :body (response/body http-response)
   :headers (response/headers->map http-response)})

(def ring-format-interceptor
  {:name ::map-format-interceptor
   :enter (fn [ctx]
            (-> (apply dissoc ctx ring-keys)
                (assoc :ring/request (select-keys ctx ring-keys))))
   :leave (fn [ctx]
            (-> ctx
                (dissoc :ring/response :ring/request)
                (conj (:ring/response ctx))))})

(def request-interceptor
  {:name ::request
   :enter (fn [ctx]
            (assoc ctx :exoscale.telex/request (ring1->http-request ctx)))
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
              (ix/when (fn [{{:keys [form-params body]} :ring/request}]
                         (and (not body)
                              (seq form-params)))))})

(def interceptor-chain
  [(interceptor/throw-on-err-status-interceptor [:status])
   ring-format-interceptor
   query-params-interceptor
   form-params-interceptor
   request-interceptor
   interceptor/send-interceptor])
