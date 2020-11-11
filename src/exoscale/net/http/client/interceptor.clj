(ns exoscale.net.http.client.interceptor
  (:require [exoscale.ex.http :as ex-http]
            exoscale.interceptor.auspex
            [exoscale.net.http.client.response :as response]
            [exoscale.net.http.client.utils :as u]
            [qbits.auspex :as ax])
  (:import (java.net.http HttpClient)))

(defn- encode-query-param
  [k v]
  [(u/url-encode (name k)) "=" (u/url-encode v)])

(defn encode-query-params
  [query-params]
  (when (seq query-params)
    (transduce (comp
                (mapcat (fn [[k v]]
                          (if (sequential? v)
                            (map #(encode-query-param k %) v)
                            [(encode-query-param k v)])))
                (interpose "&")
                cat)
               u/string-builder
               query-params)))

(def send-interceptor
  {:name ::send
   :enter (fn [{:as ctx
                :exoscale.net.http/keys [request ^HttpClient
                                         client]}]
            (let [{:exoscale.net.http.client.response/keys [executor]
                   :exoscale.net.http.client.request/keys [async?]} ctx
                  body-handler (response/body-handler ctx)]
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

;; borrowed from clj-http
(def ok-status
  #{200 201 202 203 204 205 206 207 300 301 302 303 304 307 308})

(defn throw-on-err-status-interceptor
  [status-path]
  {:name ::throw-on-error
   :leave
   (fn [ctx]
     (when (and (:exoscale.net.http.client.request/throw-on-error? ctx)
                (not (contains? ok-status
                                (response/status (:exoscale.net.http/response ctx)))))
       (ex-http/response->ex-info! (assoc ctx :status (get-in ctx status-path))))
     ctx)})
