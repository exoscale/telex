(ns exoscale.net.http.client
  (:refer-clojure :exclude [get])
  (:require [exoscale.interceptor :as ix]
            [exoscale.net.http.client.interceptor :as interceptor]
            [exoscale.net.http.client.option :as option]
            [clojure.core.async :as async])
  (:import
   (java.net.http HttpClient)))

(defn build [{:as options}]
  (let [b (HttpClient/newBuilder)]
    (option/set-options! b options)
    (.build b)))

(def default-opts
  {:exoscale.net.http.client.option/follow-redirects :normal
   :exoscale.net.http.client.option/version :http-2
   :exoscale.net.http.response/body-handler :input-stream})

(defn client
  [opts]
  (let [client (build (merge default-opts opts))]
    client))

(defn ring1-request
  ([client request]
   (ring1-request client request nil))
  ([client request ctx]
   (ix/execute (assoc ctx
                      :exoscale.net.http/client client
                      :request request)
               (:interceptor-chain ctx interceptor/default-interceptor-chain))))

(def request ring1-request)

(defn get
  [client request]
  (ring1-request client
                 (assoc request :method :get)))

(defn post
  [client request]
  (ring1-request client
                 (assoc request :method :post)))

(defn put
  [client request]
  (ring1-request client
                 (assoc request :method :put)))

(defn delete
  [client request]
  (ring1-request client
                 (assoc request :method :delete)))

;; (def c (client {}))

;; (use 'exoscale.net.http.client.core-async)
;; (def ch (clojure.core.async/chan 1))
;; (dotimes [i 10]
;;   (async/put! ch (java.nio.ByteBuffer/wrap (.getBytes (str i))))
;;   (async/close! ch))

;; (prn (request c
;;               {:method :post :url "http://0.0.0.0:8000"
;;                :query-params {:foo :bar}
;;                :body ch
;;                ;; :exoscale.net.http.response/body-handler :discarding
;;                ;; :body/handler-opts {}
;;                }))
