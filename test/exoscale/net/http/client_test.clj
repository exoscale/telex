(ns exoscale.net.http.client-test
  (:require [clojure.test :refer :all]
            exoscale.ex.test
            [exoscale.net.http.client.interceptor :as ix]
            [exoscale.net.http.client :as client]))

(def c (client/client {}))

(deftest test-simple-requests-roundrip
  (is (= 200 (:status (client/get c {:url "http://google.com"}))))
  (is (thrown-ex-info-type? :exoscale.ex/not-found
                            (client/get c {:url "http://google.com/404"})))
  (is (thrown-ex-info-type? :exoscale.ex/unsupported
                            (client/post c {:url "http://google.com/"}))))

(deftest params-test
  (is (= "a=1&b=2" (ix/encode-query-params {:a 1 :b 2})))
  (is (= "a=1" (ix/encode-query-params {:a 1})))
  (is (= "" (ix/encode-query-params nil))))
