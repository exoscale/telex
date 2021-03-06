(ns exoscale.telex.request
  (:require [clojure.string :as str]
            [exoscale.telex.enum :as enum]
            [qbits.auspex.executor :as exe])
  (:import (java.io InputStream)
           (java.net URI)
           (java.net.http
            HttpRequest
            HttpRequest$BodyPublishers
            HttpRequest$BodyPublisher
            HttpRequest$BodyPublishers)
           (java.time Duration)
           (java.util.function Supplier)))

(def default-options
  (merge #:exoscale.telex.request{:async? true
                                  :throw-on-error? true}
         #:exoscale.telex.response{:executor (exe/work-stealing-executor)}))

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
     (reify Supplier (get [_] is))))

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

(defn http-request
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
      (.POST builder (-body-publisher body))
      :put
      (.PUT builder (-body-publisher body))
      ;; else
      (.method builder
               (-> (name method) str/upper-case)
               (-body-publisher body)))

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
