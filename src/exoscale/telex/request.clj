(ns exoscale.telex.request
  (:import
   (java.io File InputStream)
   (okhttp3 Request$Builder
            Headers
            Request
            Headers$Builder
            RequestBody
            MediaType)
   (okio ByteString Okio))
  (:require [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defprotocol ToBody
  (to-body [body opts]))

(def default-media-type (MediaType/parse "application/octet-stream"))

(extend-protocol ToBody
  byte/1
  (to-body [^bytes/1 body ^MediaType media-type]
    (RequestBody/create media-type body))

  java.io.InputStream
  (to-body [^InputStream body ^MediaType media-type]
    (RequestBody/create media-type
                        ^ByteString (-> (Okio/buffer (Okio/source body))
                                        .readByteString)))
  String
  (to-body [^String body ^MediaType media-type]
    (RequestBody/create media-type ^String body))

  File
  (to-body [^File body ^MediaType media-type]
    (RequestBody/create media-type ^File body))

  Object
  (to-body [body ^MediaType media-type]
    (to-body (str body) media-type))

  nil
  (to-body [_body ^MediaType _media-type]
    nil))

(defn ->headers
  ^Headers [headers]
  (let [b (Headers$Builder/new)]
    (run! (fn [[k v]]
            (.add b
                  (name k)
                  (name v)))
          headers)
    (.build b)))

(defn ->method
  [method]
  (case method
    :get "GET"
    :post "POST"
    :put "PUT"
    :delete "DELETE"
    :head "HEAD"
    :patch "PATCH"
    :options "OPTIONS"
    (-> method name str/upper-case)))

(def media-type
  (memoize
   (fn [content-type]
     (if content-type
       (MediaType/parse content-type)
       default-media-type))))

(defn build
  ^Request
  [{:as _request :keys [method headers url body]}
   & {:as _opts}]
  (let [method (->method method)
        req (Request$Builder/new)
        headers' (->headers headers)
        ct (.get headers' "content-type")]
    (-> (doto req
          (.method method (to-body body (media-type ct)))
          (.headers (->headers headers))
          (.url ^String url))
        .build)))
