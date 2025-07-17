(ns exoscale.telex.response
  (:import (okhttp3 Response)))

(defn headers
  [^Response response]
  (-> (reduce-kv (fn [m k v]
                   (cond-> m
                     (and (sequential? v) (= 1 (count v)))
                     (assoc! k v)))
                 (transient {})
                 (.toMultimap (.headers response)))
      persistent!))

(defn body
  [^Response response]
  (-> response .body .byteStream))

(defn build
  [^Response response _opts]
  {:status (.code response)
   :headers (headers response)
   :body (body response)})
