(ns exoscale.telex
  (:require [exoscale.ex :as ex]
            [exoscale.telex.request :as request]
            [exoscale.telex.response :as response])
  (:import (java.time Duration)
           (javax.net.ssl SSLContext)
           (okhttp3 OkHttpClient
                    OkHttpClient$Builder
                    Dispatcher
                    EventListener
                    EventListener$Factory)))

(set! *warn-on-reflection* true)

(defmulti set-client-option! (fn [^OkHttpClient$Builder _b k _v] k))

(defmethod set-client-option! :follow-redirects
  [^OkHttpClient$Builder b _ v]
  (.followRedirects b v))

;; (defmethod set-client-option! :ssl-context
;;   [^OkHttpClient$Builder b _ ^SSLContext ssl-context]
;;   (set-client-option! b :ssl-socket-factory
;;                       (.getSocketFactory ssl-context)
;;                        )
;; )

(defmethod set-client-option! :add-interceptors
  [^OkHttpClient$Builder b _ interceptors]
  (doseq [ix interceptors]
    (set-client-option! b :add-interceptor ix))
  b)

(defmethod set-client-option! :add-interceptor
  [^OkHttpClient$Builder b _ interceptor]
  (.addInterceptor b interceptor))

(defmethod set-client-option! :add-network-interceptors
  [^OkHttpClient$Builder b _ interceptors]
  (doseq [ix interceptors]
    (set-client-option! b :add-network-interceptor ix))
  b)

(defmethod set-client-option! :add-network-interceptor
  [^OkHttpClient$Builder b _ interceptor]
  (.addNetworkInterceptor b interceptor))

(defmethod set-client-option! :ssl-socket-factory
  [^OkHttpClient$Builder b _ [factory trust-manager]]
  (.sslSocketFactory b factory trust-manager))

(defmethod set-client-option! :follow-ssl-redirects
  [^OkHttpClient$Builder b _ v]
  (.followSslRedirects b v))

(defmethod set-client-option! :retry-on-connection-failure
  [^OkHttpClient$Builder b _ v]
  (.retryOnConnectionFailure b v))

(defmethod set-client-option! :dispatcher
  [^OkHttpClient$Builder b _ {:as _dispatcher
                              :keys [executor
                                     max-requests
                                     max-requests-per-host]}]
  (set-client-option! b :dispatcher*
                      (let [d ^Dispatcher (Dispatcher. executor)]
                        (when max-requests
                          (.setMaxRequests d (int max-requests)))
                        (when max-requests-per-host
                          (.setMaxRequestsPerHost d (int max-requests-per-host)))
                        d)))

(defmethod set-client-option! :dispatcher*
  [^OkHttpClient$Builder b _ v]
  (.dispatcher b v))

(defmethod set-client-option! :write-timeout
  [^OkHttpClient$Builder b _ v]
  (.writeTimeout b (Duration/ofMillis v)))

(defmethod set-client-option! :read-timeout
  [^OkHttpClient$Builder b _ v]
  (.readTimeout b (Duration/ofMillis v)))

(defmethod set-client-option! :connect-timeout
  [^OkHttpClient$Builder b _ v]
  (.connectTimeout b (Duration/ofMillis v)))

(defmethod set-client-option! :call-timeout
  [^OkHttpClient$Builder b _ v]
  (.callTimeout b (Duration/ofMillis v)))

(defmethod set-client-option! :default
  [^OkHttpClient$Builder _b k v]
  (ex/ex-incorrect! "Unsupported client option"
                    {:key k
                     :value v}))

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
