(ns exoscale.telex
  (:require [exoscale.ex :as ex]
            [exoscale.telex.request :as request]
            [exoscale.telex.response :as response])
  (:import ;; (javax.net.ssl SSLContext)
   (java.security.cert CertificateFactory)
   (java.time Duration)
   (okhttp3 OkHttpClient
            OkHttpClient$Builder
            Dispatcher
            Protocol)))

(set! *warn-on-reflection* true)

(defmulti set-client-option! (fn [^OkHttpClient$Builder _b k _v] k))

(defmethod set-client-option! :follow-redirects
  [^OkHttpClient$Builder b _ v]
  (.followRedirects b v))

(defmethod set-client-option! :tls
  [^OkHttpClient$Builder b _ tls]
  (set-client-option! b :ssl-socket-factory
                      ;; (.getSocketFactory ssl-context)
                      ))
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

(defmethod set-client-option! :protocols
  [^OkHttpClient$Builder b _ v]
  (.protocols b (map Protocol/get v)))

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

(def client-options {})

(defn client
  ([^OkHttpClient client opts]
   (let [b ^OkHttpClient$Builder (.newBuilder client)]
     (-> b
         (set-client-options! opts)
         (.build))))
  ([opts]
   (let [^OkHttpClient$Builder b
         (-> (OkHttpClient$Builder/new)
             (set-client-options! (into client-options opts)))]
     (.build b))))

(def request-options {:throw-on-error true})

(defn request
  [^OkHttpClient client request-map & {:as opts}]
  (let [opts (into request-options opts)]
    (-> client
        (.newCall (request/build request-map opts))
        (.execute)
        (response/build opts))))
