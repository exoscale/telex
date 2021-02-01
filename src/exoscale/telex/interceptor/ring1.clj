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
  [{:keys [url query method body headers] :or {method :get}
    :exoscale.telex.request/keys [timeout version expect-continue?]}]
  (request/http-request url query method body headers timeout version
                        expect-continue?))

(def request-interceptor
  {:name ::request
   :enter (fn [ctx]
            (assoc ctx :exoscale.telex/request (ring1->http-request ctx)))
   :leave (fn [{:as ctx :exoscale.telex/keys [response]}]
            (-> (apply dissoc ctx request-keys)
                (assoc :status (response/status response)
                       :body (response/body response)
                       :headers (response/headers->map response))))})

(def query-params-interceptor
  {:name ::query-params
   :enter
   (-> interceptor/encode-query-params
       (ix/in [:query-params])
       (ix/out [:query]))})

(def form-params-interceptor
  {:name ::form-params
   :enter (-> interceptor/encode-query-params
              (ix/in [:form-params])
              (ix/out [:body])
              (ix/when (fn [ctx]
                         (and (not (:body ctx))
                              (seq (:form-params ctx))))))})

(def interceptor-chain
  [(interceptor/throw-on-err-status-interceptor [:status])
   query-params-interceptor
   form-params-interceptor
   request-interceptor
   interceptor/send-interceptor])
