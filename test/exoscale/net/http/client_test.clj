(ns exoscale.net.http.client-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            exoscale.ex.test
            [exoscale.net.http.client.interceptor :as ix]
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
                            (request {:method :get :url "http://google.com/404"})))
  (is (thrown-ex-info-type? :exoscale.ex/unsupported
                            (request {:method :post :url "http://google.com"}))))

(deftest params-test
  (is (= "a=1&b=2" (ix/encode-query-params {:a 1 :b 2})))
  (is (= "a=1" (ix/encode-query-params {:a 1})))
  (is (= nil (ix/encode-query-params nil))))
