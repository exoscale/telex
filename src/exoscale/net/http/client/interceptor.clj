(ns exoscale.net.http.client.interceptor
  (:require [clojure.string :as str]
            [exoscale.ex.http :as ex-http]
            [exoscale.interceptor :as ix]
            exoscale.interceptor.auspex
            [exoscale.net.http.client.enum :as enum]
            [exoscale.net.http.client.request :as request]
            [exoscale.net.http.client.response :as response]
            [exoscale.net.http.client.utils :as u]
            [qbits.auspex :as ax]
            [qbits.auspex.executor :as exe]
            [clojure.core.async :as async])
  (:import (java.net URI)
           (java.net.http HttpClient
                          HttpRequest
                          HttpResponse)
           (java.time Duration)))

(defn encode-query-params
  [query-params]
  (transduce (comp (map (fn [[k v]]
                          [(name k) "=" (u/url-encode v)]))
                   (interpose "&")
                   cat)
             u/string-builder
             query-params))

(defn- ring1->http-request
  ^HttpRequest
  [{:keys [url query method body headers timeout version
           expect-continue?]
    :or {method :get}}]
  (let [builder (doto (HttpRequest/newBuilder)
                  (.uri (URI. (cond-> url
                                query
                                (str "?" query)))))]
    (case method
      :get
      (.GET builder)
      :delete
      (.DELETE builder)
      :post
      (.POST builder (request/-body-publisher body))
      :put
      (.PUT builder (request/-body-publisher body))
      ;; else
      (.method builder
               (-> (name method) str/upper-case)
               (request/-body-publisher body)))

    (run! (fn [[k v]]
            (.header builder (name k) (str v)))
          headers)

    (cond-> builder
      version
      (.version (enum/version version))
      timeout
      (.timeout (Duration/ofMillis timeout))
      expect-continue?
      (.expectContinue expect-continue?)
      :then (.build))))

(defn- http-response->ring1
  [^HttpResponse http-response]
  {:status (response/status http-response)
   :body (response/body http-response)
   :headers (response/headers->map http-response)})

(def ring1-interceptor
  {:name ::ring1-request
   :enter (fn [{:as ctx :keys [request]}]
            (assoc ctx :exoscale.net.http/request (ring1->http-request request)))
   :leave (fn [{:as ctx :exoscale.net.http/keys [response]}]
            (assoc ctx :response (http-response->ring1 response)))})

(def send-interceptor
  {:name ::send
   :enter (fn [{:as ctx
                :exoscale.net.http/keys [request ^HttpClient
                                         client
                                         response-handler-executor]
                :or {response-handler-executor (exe/fork-join-executor)}}]
            (let [{:exoscale.net.http.response/keys [handler handler-opts]
                   :or {handler :input-stream}} ctx
                  body-handler (response/body-handler handler handler-opts)]
              (if (-> ctx :request :async?)
                (-> (.sendAsync client request body-handler)
                    (ax/then (fn [response]
                               (assoc ctx
                                      :exoscale.net.http/response response))
                             response-handler-executor))
                (assoc ctx
                       :exoscale.net.http/response
                       (.send client
                              request
                              body-handler)))))})

;; stolen from clj-http
(def ok-status
  #{200 201 202 203 204 205 206 207 300 301 302 303 304 307 308})

(def throw-on-err-status-interceptor
  {:leave (fn [{:as ctx :exoscale.net.http/keys [response]}]
            (cond-> ctx
              (not (contains? ok-status
                              (response/status response)))
              (ex-http/response->ex-info!)))})

(def default-interceptor-chain
  [{:leave (fn [ctx] (:response ctx))}
   ;; throw-on-err-status-interceptor
   {:enter (-> encode-query-params
               (ix/in [:request :query-params])
               (ix/out [:request :query]))}
   #'ring1-interceptor
   #'send-interceptor])
