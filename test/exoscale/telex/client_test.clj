(ns exoscale.telex.client-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            exoscale.ex.test
            [exoscale.ex :as ex]
            [exoscale.telex.mocks :as mocks]
            [exoscale.telex.interceptor :as ix]
            [exoscale.telex.interceptor.ring1 :as r1]
            [exoscale.telex :as client]))

(def ^:dynamic client)
(def ^:dynamic request-opts)
(def ^:dynamic request)

(use-fixtures :once
  (fn [t]
    (binding [client (client/client {})
              request-opts (merge #:exoscale.telex.request{:async? false})
              request #(client/request client (merge request-opts %))]
      (t))))

(deftest test-simple-requests-roundrip
  (is (= 200 (:status (request {:method :get :url "http://google.com"}))))

  (is (= 200 (:status (request {:method :get :url "http://google.com"
                                :exoscale.telex.request/version :http-2}))))

  (is (= 200 (:status (request {:method :get :url "http://google.com"
                                :exoscale.telex.request/version :http-1-1}))))

  (is (thrown? java.net.http.HttpTimeoutException
               (request {:method :get :url "http://google.com"
                         :exoscale.telex.request/timeout 1})))

  (is (thrown-ex-info-type? :exoscale.ex/not-found
                            (request {:method :get :url "http://google.com/404"}))
      "errors are mapped correctly for GET")

  (is (thrown-ex-info-type? :exoscale.ex/unsupported
                            (request {:method :post :url "http://google.com"}))
      "errors are mapped correctly for GET")

  (is (= 405 (:status (request {:method :post :url "http://google.com"
                                :exoscale.telex.request/throw-on-error? false})))
      "disabling the throw interceptor"))

(deftest test-body-handler
  (mocks/with-server 1234 (constantly {:status 200
                                       :body "Some value"})
    (let [{:keys [status body]} (request {:method :get
                                          :url "http://localhost:1234"
                                          :exoscale.telex.response/body-handler :string})]
      (is (= 200 status))
      (is (= "Some value" body)))))

(deftest test-post-body
  (mocks/with-server 1234 (fn [{:keys [body]}] {:status 200 :body body})
    (let [{:keys [status body]}
          (request {:method :post
                    :url "http://localhost:1234"
                    :exoscale.telex.response/body-handler :string
                    :body "abc"})]
      (is (= 200 status))
      (is (= "abc" body)))))

(deftest test-post-form-params
  (mocks/with-server 1234 (fn [{:keys [body]}] {:status 200 :body body})
    (let [{:keys [status body]}
          (request {:method :post
                    :url "http://localhost:1234"
                    :exoscale.telex.response/body-handler :string
                    :form-params {:a 1}})]
      (is (= 200 status))
      (is (= "a=1" body)))))

(deftest test-post-form-params+body
  (mocks/with-server 1234 (fn [{:keys [body]}] {:status 200 :body body})
    (let [{:keys [status body]}
          (request {:method :post
                    :url "http://localhost:1234"
                    :exoscale.telex.response/body-handler :string
                    :form-params {:a 1}
                    :body "a"})]
      (is (= 200 status))
      (is (= "a" body) "body always takes over if specified"))))

(deftest test-error-handling
  (mocks/with-server 1234 (constantly {:status 400
                                       :body "Invalid"})
    (ex/try+
     (request {:method :get
               :url "http://localhost:1234"
               :exoscale.telex.response/body-handler :string})
     (catch :exoscale.ex/incorrect {{:keys [status body]} :response}
       (is (= status 400))
       (is (= body "Invalid"))))))

(deftest params-test
  (is (= "a=1&b=2" (ix/encode-query-params {:a 1 :b 2})))
  (is (= "a=1" (ix/encode-query-params {:a 1})))
  (is (= "a=1&b=2&b=3&b=4&c=5" (ix/encode-query-params {:a 1 :b [2 3 4] :c 5})))
  (is (= nil (ix/encode-query-params nil))))
