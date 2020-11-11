(ns exoscale.net.http.client-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            exoscale.ex.test
            [exoscale.net.http.client.interceptor :as ix]
            [exoscale.net.http.client.interceptor.ring1 :as r1]
            [exoscale.net.http.client :as client]))

(def ^:dynamic client)
(def ^:dynamic request-opts)
(def ^:dynamic request)

(use-fixtures :once
  (fn [t]
    (binding [client (client/client {})
              request-opts (merge #:exoscale.net.http.client.request{:async? false})
              request #(client/request client (merge request-opts %))]
      (t))))

(deftest test-simple-requests-roundrip
  (is (= 200 (:status (request {:method :get :url "http://google.com"}))))

  (is (thrown-ex-info-type? :exoscale.ex/not-found
                            (request {:method :get :url "http://google.com/404"}))
      "errors are mapped correctly for GET")

  (is (thrown-ex-info-type? :exoscale.ex/unsupported
                            (request {:method :post :url "http://google.com"}))
      "errors are mapped correctly for GET")

  (is (= 405 (:status (request {:method :post :url "http://google.com"
                                :exoscale.net.http.client.request/throw-on-error? false})))
      "disabling the throw interceptor"))

(deftest params-test
  (is (= "a=1&b=2" (ix/encode-query-params {:a 1 :b 2})))
  (is (= "a=1" (ix/encode-query-params {:a 1})))
  (is (= "a=1&b=2&b=3&b=4&c=5" (ix/encode-query-params {:a 1 :b [2 3 4] :c 5})))
  (is (= nil (ix/encode-query-params nil))))
