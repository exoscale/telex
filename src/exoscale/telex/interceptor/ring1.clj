(ns exoscale.telex.interceptor.ring1
  (:require [exoscale.interceptor :as ix]
            [exoscale.telex.interceptor :as interceptor]
            [exoscale.telex.request :as request]
            [exoscale.telex.response :as response])
  (:import (java.net.http HttpRequest
                          HttpResponse)))

(def request-keys [:url :body :method :query :headers :url
                   ;; exts
                   :form-params :query-params])

(defn- ring1->http-request
  ^HttpRequest
  [{:ring1/keys [request]
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
            (-> (apply dissoc ctx request-keys)
                (assoc :ring1/request (select-keys ctx request-keys))))
   :leave (fn [ctx]
            ;; we just care about the final request output, hide
            ;; original request from potential output
            (-> ctx
                (dissoc :ring1/response :ring1/request)
                (conj (:ring1/response ctx))))})

(def request-interceptor
  {:name ::request
   :enter (fn [ctx]
            (assoc ctx
                   :exoscale.telex/request (ring1->http-request ctx)))
   :leave (fn [ctx]
            (assoc ctx
                   :ring1/response
                   (http-response->ring1 (:exoscale.telex/response ctx))))})

(def query-params-interceptor
  {:name ::query-params
   :enter
   (-> interceptor/encode-query-params
       (ix/in [:ring1/request :query-params])
       (ix/out [:ring1/request :query]))})

(def form-params-interceptor
  {:name ::form-params
   :enter (-> interceptor/encode-query-params
              (ix/in [:ring1/request :form-params])
              (ix/out [:ring1/request :body])
              (ix/when (fn [{:ring1/keys [request]}]
                         (and (not (:body request))
                              (seq (:form-params request))))))})

(def interceptor-chain
  [(interceptor/throw-on-err-status-interceptor [:status])
   ring-format-interceptor
   query-params-interceptor
   form-params-interceptor
   request-interceptor
   interceptor/send-interceptor])
