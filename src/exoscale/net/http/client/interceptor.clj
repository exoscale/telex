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

(defn make-request
  [url query method body headers timeout version expect-continue?]
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

(defn- ring1->http-request
  ^HttpRequest
  [{:keys [url query method body headers timeout version
           expect-continue?]
    :or {method :get}}]
  (make-request url query method body headers timeout version
                expect-continue?))

(defn- ring2->http-request
  ^HttpRequest
  [{:ring.request/keys [url query method body headers timeout version
                        expect-continue?]
    :or {method :get}}]
  (make-request url query method body headers timeout version
                expect-continue?))

(defn- http-response->ring1
  [^HttpResponse http-response]
  {:status (response/status http-response)
   :body (response/body http-response)
   :headers (response/headers->map http-response)})

(defn- http-response->ring2
  [^HttpResponse http-response]
  #:ring.response{:status (response/status http-response)
                  :body (response/body http-response)
                  :headers (response/headers->map http-response)})

(def ring1-interceptor
  {:name ::ring1
   :enter (fn [{:as ctx :keys [request]}]
            (assoc ctx :exoscale.net.http/request (ring1->http-request request)))
   :leave (fn [{:as ctx :exoscale.net.http/keys [response]}]
            (assoc ctx :response (http-response->ring1 response)))})

(def ring2-interceptor
  {:name ::ring2
   :enter (fn [ctx]
            (assoc ctx :exoscale.net.http/request (ring2->http-request ctx)))
   :leave (fn [ctx]
            (into ctx (http-response->ring2 ctx)))})

(def send-interceptor
  {:name ::send
   :enter (fn [{:as ctx
                :exoscale.net.http/keys [request ^HttpClient
                                         client
                                         response-handler-executor]
                :or {response-handler-executor (exe/fork-join-executor)}}]
            (let [{:exoscale.net.http.client.response/keys [body-handler handler-opts]
                   :exoscale.net.http.client.request/keys [async?]
                   :or {body-handler :input-stream}} ctx
                  body-handler (response/body-handler body-handler handler-opts)]
              (if async?
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

(defn throw-on-err-status-interceptor
  [response-path]
  {:leave (fn [{:as ctx :exoscale.net.http/keys [response]}]
            (when-not (contains? ok-status
                                 (response/status response))
              (ex-http/response->ex-info! (get-in ctx response-path)))
            ctx)})

(def ring1-interceptor-chain
  [{:leave (fn [ctx] (:response ctx))}
   (throw-on-err-status-interceptor [:response])
   {:enter (-> encode-query-params
               (ix/in [:request :query-params])
               (ix/out [:request :query]))}
   #'ring1-interceptor
   #'send-interceptor])

(def ring2-interceptor-chain
  [{:leave (fn [ctx] (:response ctx))}
   (throw-on-err-status-interceptor [:response])
   {:enter (-> encode-query-params
               (ix/in [:request :query-params])
               (ix/out [:request :query]))}
   #'ring2-interceptor
   #'send-interceptor])

(def default-interceptor-chain ring1-interceptor-chain)
