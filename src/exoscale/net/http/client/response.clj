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

(defmulti body-handler (fn [k _opts] k))

(defmethod body-handler :discarding
  [_ _]
  (HttpResponse$BodyHandlers/discarding))

(defmethod body-handler :string
  [_ _]
  (HttpResponse$BodyHandlers/ofString))

(defmethod body-handler :byte-array
  [_ h]
  (HttpResponse$BodyHandlers/ofByteArray))

(defmethod body-handler :input-stream
  [_ _]
  (HttpResponse$BodyHandlers/ofInputStream))

(defmethod body-handler :publisher
  [_ _]
  (HttpResponse$BodyHandlers/ofPublisher))

(defmethod body-handler :byte-array-consumer
  [_ {:keys [consumer]}]
  (HttpResponse$BodyHandlers/ofByteArrayConsumer consumer))

(defmethod body-handler :file
  [_ {:keys [file]}]
  (HttpResponse$BodyHandlers/ofFile file))

(defmethod body-handler :file-download
  [_ {:keys [path opts]}]
  (HttpResponse$BodyHandlers/ofFileDownload path opts))

(defmethod body-handler :subscriber
  [_ {:keys [subscriber]}]
  (HttpResponse$BodyHandlers/fromSubscriber subscriber))

(defmethod body-handler :line-subscriber
  [_ {:keys [subscriber]}]
  (HttpResponse$BodyHandlers/fromLineSubscriber subscriber))

(defmethod body-handler :buffering
  [_ {:keys [buffer buffer-size]}]
  (HttpResponse$BodyHandlers/buffering buffer
                                       buffer-size))

(defmethod body-handler :replacing
  [_ {:keys [value]}]
  (HttpResponse$BodyHandlers/replacing value))

(defmethod body-handler :lines
  [_ _]
  (HttpResponse$BodyHandlers/ofLines))

(defmethod body-handler :default
  [_ x]
  (cond
    (ifn? x) (x)
    (instance? HttpResponse$BodyHandler x) x))

(defn headers->map
  [^HttpResponse http-response]
  (into {}
        (map (fn [[k v]]
               [k (cond-> v (= (count v) 1) first)]))
        (headers http-response)))
