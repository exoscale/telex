(ns exoscale.net.http.client
  (:refer-clojure :exclude [get])
  (:require [exoscale.interceptor :as ix]
            [exoscale.net.http.client.interceptor :as interceptor]
            [exoscale.net.http.client.interceptor.ring1 :as ring1]
            [exoscale.net.http.client.interceptor.ring2 :as ring2]
            [exoscale.net.http.client.option :as option])
  (:import
   (java.net.http HttpClient)))

(defn build [{:as options}]
  (let [b (HttpClient/newBuilder)]
    (option/set-options! b options)
    (.build b)))

(def default-opts
  {:exoscale.net.http.client.option/follow-redirects :normal
   :exoscale.net.http.client.option/version :http-2
   :exoscale.net.http.client.response/body-handler :input-stream})

(defn client
  [opts]
  (let [client (build (merge default-opts opts))]
    client))

(defn ring1-request
  [client ctx]
  (let [ctx (reduce-kv (fn [m k v]
                         (assoc m
                                (if (namespace k)
                                  k
                                  (keyword "ring.request" (name k)))
                                v))
                       ctx
                       ctx)]
    (ix/execute (assoc ctx :exoscale.net.http/client client)
                (:interceptor-chain ctx ring1/interceptor-chain))))

(defn ring2-request
  [client ctx]
  (ix/execute (assoc ctx
                     :exoscale.net.http/client client)
              (:interceptor-chain ctx ring2/interceptor-chain)))

(def request ring1-request)

;; (def c (client {}))
;; (prn @(request c
;;                {:method :get
;;                 :url "http://google.com/"
;;                 :query-params {:foo :bar}
;;                 :exoscale.net.http.client.request/async? true
;;                 :exoscale.net.http.client.response/body-handler :discarding}))
