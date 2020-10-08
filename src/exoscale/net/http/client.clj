(ns exoscale.net.http.client
  (:refer-clojure :exclude [get])
  (:require [exoscale.interceptor :as ix]
            [exoscale.net.http.client.interceptor.ring1 :as ring1]
            [exoscale.net.http.client.interceptor.ring2 :as ring2]
            [exoscale.net.http.client.option :as option]
            [qbits.auspex.executor :as exe])
  (:import
   (java.net.http HttpClient)))

(defn build [{:as options}]
  (let [b (HttpClient/newBuilder)]
    (option/set-options! b options)
    (.build b)))

(def default-opts
  {:exoscale.net.http.client.option/follow-redirects :normal
   :exoscale.net.http.client.option/version :http-2
   :exoscale.net.http.client.response/body-handler :input-stream
   :exoscale.net.http.client.response/executor (exe/work-stealing-executor)})

(defn client
  [opts]
  (let [client (build (merge default-opts opts))]
    client))

(defn ring1-request
  [client ctx]
  ;; set the ring1 request to its own namespace
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
  (ix/execute (assoc ctx :exoscale.net.http/client client)
              (:interceptor-chain ctx ring2/interceptor-chain)))

(def request ring1-request)

(defn- method-fn
  [method]
  (fn req
    ([client url]
     (req client url {}))
    ([client url r]
     (request client
              (assoc r
                     :method method
                     :url url)))))

(def get (method-fn :get))
(def post (method-fn :post))
(def put (method-fn :put))
(def delete (method-fn :delete))

;; (def c (client {}))
;; (prn @(request c
;;                {:method :get
;;                 :url "http://google.com/"
;;                 :query-params {:foo :bar}
;;                 :form-params {:foo :bar1}
;;                 :exoscale.net.http.client.request/async? true
;;                 ;; :exoscale.net.http.client.response/body-handler :discarding
;;                 }))
