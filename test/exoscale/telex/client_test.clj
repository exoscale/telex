(ns exoscale.telex.client-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [exoscale.ex :as ex]
            exoscale.ex.test
            [exoscale.telex :as client]
            [exoscale.telex.mocks :as mocks])
  (:import (java.net SocketTimeoutException)))

(def large-file "https://ash-speed.hetzner.com/1GB.bin")

(def ^:dynamic *client* nil)
(def ^:dynamic *client-opts* {})
(def ^:dynamic request nil)

(use-fixtures :once
  (fn [t]
    (binding [*client* (client/client *client-opts*)
              request (fn [req & {:as opts}] (client/request *client* req opts))]
      (t))))

(deftest test-simple-requests-roundrip
  (is (= 200 (:status (request {:method :get :url "http://google.com"}))))

  (is (= 200 (:status (request {:method :get :url "http://google.com"}))))

  (is (= 200 (:status (request {:method :get :url "http://google.com"}))))

  (is (thrown-ex-info-type? :exoscale.ex/not-found
                            (request {:method :get :url "http://google.com/404"}))
      "errors are mapped correctly for GET")

  (is (thrown-ex-info-type? :exoscale.ex/unsupported
                            (request {:method :post :url "http://google.com"
                                      :body ""}))
      "errors are mapped correctly for POST")

  (is (= 405 (:status (request {:method :post :url "http://google.com"
                                :body ""}
                               {:throw-on-error false})))
      "disabling the throw opts"))

(deftest test-timeout
  (binding [*client-opts* {:read-timeout 1}]
    (is (thrown? SocketTimeoutException
                 (client/request (client/client *client-opts*)
                                 {:method :get :url "http://google.com"})))))

(deftest test-body-handler
  (mocks/with-server 1234 (constantly {:status 200
                                       :body "Some value"})
    (let [{:keys [status body]} (request {:method :get
                                          :url "http://localhost:1234"}
                                         :response-body-decoder :string)]
      (is (= 200 status))
      (is (= "Some value" body)))))

(deftest test-post-body
  (mocks/with-server 1234 (fn [{:keys [body]}] {:status 200 :body body})
    (let [{:keys [status body]}
          (request {:method :post
                    :url "http://localhost:1234"
                    :body "abc"}
                   :response-body-decoder :string)]
      (is (= 200 status))
      (is (= "abc" body)))))

#_(deftest test-post-form-params
    (mocks/with-server 1234 (fn [{:keys [body]}] {:status 200 :body body})
      (let [{:keys [status body]}
            (request {:method :post
                      :url "http://localhost:1234"}
                     :response-body-decoder :string)]
        (is (= 200 status))
        (is (= "a=1" body)))

      (let [{:keys [status body]}
            (request {:method :post
                      :url "http://localhost:1234"
                      :exoscale.telex.response/body-handler :string
                      :form-params {}})]
        (is (= 200 status))
        (is (= "" body)))))

#_(deftest test-post-form-params+body
    (mocks/with-server 1234 (fn [{:keys [body]}] {:status 200 :body body})
      (let [{:keys [status body]}
            (request {:method :post
                      :url "http://localhost:1234"
                      :exoscale.telex.response/body-handler :string
                      :form-params {:a 1}
                      :body "a"})]
        (is (= 200 status))
        (is (= "a" body) "body always takes over if specified"))

      (let [{:keys [status body]}
            (request {:method :post
                      :url "http://localhost:1234"
                      :exoscale.telex.response/body-handler :string
                      :form-params {}
                      :body "a"})]
        (is (= 200 status))
        (is (= "a" body)))))

(deftest test-error-handling
  (mocks/with-server 1234 (constantly {:status 400
                                       :body "Invalid"})
    (ex/try+
      (request {:method :get
                :url "http://localhost:1234"}
               :response-body-decoder :string)
      (catch :exoscale.ex/incorrect {{:keys [status body]} :response}
        (is (= status 400))
        (is (= body "Invalid"))))))

(deftest test-response-body-read-timeout
  (binding [*client-opts* {:read-timeout 2}]
    (is (thrown? java.io.IOException
                 (request {:method :get
                           :url large-file}
                          :response-body-decoder :string
                          :read-timeout 2))
        "when we try to realize we will get the actual exception"))
  (binding [*client-opts* {:read-timeout 10}]
    (is (thrown? java.io.IOException
                 (-> (request {:method :get
                               :url large-file})
                     :body
                     slurp))
        "input stream will just close in that case, HttpReadTimeoutException will be in cause, IOE is root"))
  (mocks/with-server 1234 (constantly {:status 200
                                       :body "ok"})
    (binding [*client-opts* {:read-timeout 3000}]
      (is (seq (slurp (:body (request {:method :get
                                       :url "http://localhost:1234"}))))
          "we got content before timeout"))))

;; (deftest params-test
;;   (is (= "a=1&b=2" (ix/encode-query-params {:a 1 :b 2})))
;;   (is (= "a=1" (ix/encode-query-params {:a 1})))
;;   (is (= "a=1&b=2&b=3&b=4&c=5" (ix/encode-query-params {:a 1 :b [2 3 4] :c 5})))
;;   (is (= nil (ix/encode-query-params nil))))
