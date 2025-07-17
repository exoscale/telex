(ns exoscale.telex
  (:require [exoscale.telex.request :as request]
            [exoscale.telex.response :as response])
  (:import (okhttp3 OkHttpClient
                    OkHttpClient$Builder)))

(set! *warn-on-reflection* true)

(defmulti set-client-option! (fn [^OkHttpClient$Builder _b k _v] k))

(defmethod set-client-option! :follow-redirects
  [^OkHttpClient$Builder b _ v]
  (.followRedirects b v))

(defmethod set-client-option! :follow-ssl-redirects
  [^OkHttpClient$Builder b _ v]
  (.followSslRedirects b v))

(defmethod set-client-option! :retry-on-connection-failure
  [^OkHttpClient$Builder b _ v]
  (.retryOnConnectionFailure b v))

(defmethod set-client-option! :dispatcher
  [^OkHttpClient$Builder b _ v]
  (.dispatcher b v))

(defmethod set-client-option! :write-timeout
  [^OkHttpClient$Builder b _ v]
  (.writeTimeout b v))

(defmethod set-client-option! :read-timeout
  [^OkHttpClient$Builder b _ v]
  (.readTimeout b v))

(defmethod set-client-option! :connect-timeout
  [^OkHttpClient$Builder b _ v]
  (.connectTimeout b v))

(defmethod set-client-option! :call-timeout
  [^OkHttpClient$Builder b _ v]
  (.callTimeout b v))

(defn set-client-options!
  ^OkHttpClient$Builder
  [^OkHttpClient$Builder builder opts]
  (reduce
   (fn [builder [k v]]
     (set-client-option! builder k v))
   builder
   opts))

(defn client
  [opts]
  (let [^OkHttpClient$Builder b
        (-> (OkHttpClient$Builder/new)
            (set-client-options! opts))]
    (.build b)))

(defn from-client
  [^OkHttpClient client opts]
  (let [b ^OkHttpClient$Builder (.newBuilder client)]
    (-> b
        (set-client-options! opts)
        (.build))))

(defn request
  ([^OkHttpClient client request-map]
   (request client request-map nil))
  ([^OkHttpClient client request-map opts]
   (-> client
       (.newCall (request/build request-map opts))
       (.execute)
       (response/build opts))))

;; (def c (client {}))
;; (slurp (:body (request c {:uri "http://google.com" :method :get})))
