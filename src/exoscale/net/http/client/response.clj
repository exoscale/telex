(ns exoscale.net.http.client.response
  (:import (java.net.http HttpResponse
                          HttpResponse$BodyHandlers
                          HttpResponse$BodyHandler
                          HttpResponse$BodyHandlers)))

(defprotocol Response
  (status [http-response] "Returns http status of underlying response")
  (body [http-response] "Returns response body")
  (headers [http-response] "Returns response header map"))

(extend-protocol Response
  HttpResponse
  (status [http-response]
    (.statusCode http-response))

  (body [http-response]
    (.body http-response))

  (headers [http-response]
    (-> http-response .headers .map)))

(defmulti body-handler :exoscale.net.http.client.response/body-handler)

(defmethod body-handler :discarding
  [_ctx]
  (HttpResponse$BodyHandlers/discarding))

(defmethod body-handler :string
  [{:exoscale.net.http.client.response.body-handler/keys [charset]
    :or {charset "UTF-8"}}]
  (HttpResponse$BodyHandlers/ofString charset))

(defmethod body-handler :byte-array
  [_ctx]
  (HttpResponse$BodyHandlers/ofByteArray))

(defmethod body-handler :input-stream
  [_ctx]
  (HttpResponse$BodyHandlers/ofInputStream))

(defmethod body-handler :publisher
  [_ctx]
  (HttpResponse$BodyHandlers/ofPublisher))

(defmethod body-handler :byte-array-consumer
  [{:exoscale.net.http.client.response.body-handler/keys [byte-array-consumer]}]
  (HttpResponse$BodyHandlers/ofByteArrayConsumer byte-array-consumer))

(defmethod body-handler :file
  [{:exoscale.net.http.client.response.body-handler/keys [file]}]
  (HttpResponse$BodyHandlers/ofFile file))

(defmethod body-handler :file-download
  [{:exoscale.net.http.client.response.body-handler/keys [path opts]}]
  (HttpResponse$BodyHandlers/ofFileDownload path opts))

(defmethod body-handler :subscriber
  [{:exoscale.net.http.client.response.body-handler/keys [subscriber]}]
  (HttpResponse$BodyHandlers/fromSubscriber subscriber))

(defmethod body-handler :line-subscriber
  [{:exoscale.net.http.client.response.body-handler/keys [subscriber]}]
  (HttpResponse$BodyHandlers/fromLineSubscriber subscriber))

(defmethod body-handler :buffering
  [{:exoscale.net.http.client.response.body-handler/keys [buffer buffer-size]}]
  (HttpResponse$BodyHandlers/buffering buffer
                                       buffer-size))

(defmethod body-handler :replacing
  [{:exoscale.net.http.client.response.body-handler/keys [value]}]
  (HttpResponse$BodyHandlers/replacing value))

(defmethod body-handler :lines
  [_ctx]
  (HttpResponse$BodyHandlers/ofLines))

(defmethod body-handler :default
  [ctx]
  (let [bh (:exoscale.net.http.client.response/body-handler ctx)]
    (cond
      (fn? bh) (bh)
      (instance? HttpResponse$BodyHandler bh) bh
      :else (HttpResponse$BodyHandlers/ofInputStream))))

(defn headers->map
  [^HttpResponse http-response]
  (into {}
        (map (fn [[k v]]
               [k (cond-> v (= (count v) 1) first)]))
        (headers http-response)))
