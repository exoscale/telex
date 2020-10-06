(ns exoscale.net.http.client.request
  (:import (java.io InputStream)
           (java.util.function Supplier)
           (java.net.http
            HttpRequest$BodyPublishers
            HttpRequest$BodyPublisher
            HttpRequest$BodyPublishers)))

(defprotocol BodyPublisher
  (-body-publisher [x]))

(extend-protocol BodyPublisher
  (Class/forName "[B")
  (-body-publisher [ba]
    (HttpRequest$BodyPublishers/ofByteArray ba))

  String
  (-body-publisher [s]
    (HttpRequest$BodyPublishers/ofString s))

  InputStream
  (-body-publisher [is]
    (HttpRequest$BodyPublishers/ofInputStream
     (reify Supplier
       (get [_] is))))

  HttpRequest$BodyPublisher
  (-body-publisher [p]
    (HttpRequest$BodyPublishers/fromPublisher p))

  Iterable
  (-body-publisher [it]
    (HttpRequest$BodyPublishers/ofByteArrays it))

  Object
  (-body-publisher [s]
    (-body-publisher (str s)))

  nil
  (-body-publisher [_]
    (HttpRequest$BodyPublishers/noBody)))
