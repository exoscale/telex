(ns exoscale.telex.utils
  (:import (java.net URLEncoder)))

(defn string-builder
  ([] (StringBuilder.))
  ([^StringBuilder sb x] (.append sb x))
  ([^StringBuilder sb] (.toString sb)))

(defn url-encode
  [s]
  (URLEncoder/encode (str s)
                     "UTF-8"))
