(ns exoscale.telex.option
  (:require [exoscale.telex.enum :as enum])
  (:import (java.net.http HttpClient$Builder)
           (java.time Duration)))

(defmulti set-option! (fn [k _builder _option] k))

(defmethod set-option! :exoscale.telex/authenticator
  [_ ^HttpClient$Builder b authenticator]
  (.authenticator b authenticator))

(defmethod set-option! :exoscale.telex/connect-timeout
  [_ ^HttpClient$Builder b timeout-ms]
  (.connectTimeout b (Duration/ofMillis timeout-ms)))

(defmethod set-option! :exoscale.telex/cookie-handler
  [_ ^HttpClient$Builder b cookie-handler]
  (.cookieHandler b cookie-handler))

(defmethod set-option! :exoscale.telex/executor
  [_ ^HttpClient$Builder b executor]
  (.executor b executor))

(defmethod set-option! :exoscale.telex/follow-redirects
  [_ ^HttpClient$Builder b follow-redirects]
  (.followRedirects b (enum/redirect follow-redirects)))

(defmethod set-option! :exoscale.telex/priority
  [_ ^HttpClient$Builder b priority]
  (.priority b (int priority)))

(defmethod set-option! :exoscale.telex/proxy
  [_ ^HttpClient$Builder b proxy-selector]
  (.proxy b proxy-selector))

(defmethod set-option! :exoscale.telex/ssl-context
  [_ ^HttpClient$Builder b ssl-context]
  (.sslContext b ssl-context))

(defmethod set-option! :exoscale.telex/ssl-parameters
  [_ ^HttpClient$Builder b ssl-parameters]
  (.sslParameters b ssl-parameters))

(defmethod set-option! :exoscale.telex/version
  [_ ^HttpClient$Builder b version]
  (.version b (enum/version version)))

(defmethod set-option! :default
  [_ b _]
  b)

(defn set-options!
  [builder options]
  (reduce (fn [builder [k option]]
            (set-option! k builder option))
          builder
          options))
