(ns exoscale.net.http.client.interceptor
  (:require [exoscale.ex.http :as ex-http]
            exoscale.interceptor.auspex
            [exoscale.net.http.client.response :as response]
            [exoscale.net.http.client.utils :as u]
            [qbits.auspex :as ax]
            [qbits.auspex.executor :as exe])
  (:import (java.net.http HttpClient)))

(defn encode-query-params
  [query-params]
  (when (seq query-params)
    (transduce (comp (map (fn [[k v]]
                            [(name k) "=" (u/url-encode v)]))
                     (interpose "&")
                     cat)
               u/string-builder
               query-params)))

(def send-interceptor
  {:name ::send
   :enter (fn [{:as ctx
                :exoscale.net.http/keys [request ^HttpClient
                                         client]
                :exoscale.net.http.response/keys [executor]}]
            (let [{:exoscale.net.http.client.response/keys [body-handler handler-opts]
                   :exoscale.net.http.client.request/keys [async?]
                   :or {body-handler :input-stream}} ctx
                  body-handler (response/body-handler body-handler handler-opts)]
              (if async?
                (-> (.sendAsync client request body-handler)
                    (ax/then (fn [response]
                               (assoc ctx
                                      :exoscale.net.http/response response))
                             executor))
                (assoc ctx
                       :exoscale.net.http/response
                       (.send client
                              request
                              body-handler)))))})

;; stolen from clj-http
(def ok-status
  #{200 201 202 203 204 205 206 207 300 301 302 303 304 307 308})

(defn throw-on-err-status-interceptor
  [status-path]
  {:leave (fn [ctx]
            (when-not (contains? ok-status
                                 (response/status (:exoscale.net.http/response ctx)))
              (ex-http/response->ex-info! (assoc ctx :status (get-in ctx status-path))))
            ctx)})
