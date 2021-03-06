(ns eponai.server.auth-test
  (:require [clojure.test :as test :refer [deftest is are]]
            [com.stuartsierra.component :as component]
            [eponai.server.core :as core]
            [clj-http.client :as http]
            [aleph.netty :as netty]
            [ring.util.response :as response]
            [eponai.common.routes :as routes]
            [eponai.server.test-util :as util]
            [eponai.server.datomic.mocked-data :as mocked-data]
            [taoensso.timbre :as timbre :refer [debug]]
            [eponai.common.database :as db]
            [eponai.common.location :as location]
            [eponai.common :as c]
            [cemerick.url :as url]
            [eponai.server.auth :as auth]))

(def ^:dynamic *system* nil)
(def user-email mocked-data/test-user-email)

(defn with-system [test-fn]
  (binding [*system* (timbre/with-level :error
                                        (-> (core/system-for-tests)
                                            (component/start-system)))]
    (try
      (test-fn)
      (finally
        (timbre/with-level :error (component/stop *system*))))))

(test/use-fixtures :each with-system)

(defn location-cookie []
  (let [value {:value (url/url-encode (c/write-transit {:sulo-locality/path "yvr"}))}]
    {location/locality-cookie-name value}))

(deftest test-not-authed-http
  (let [red (:found response/redirect-status-codes)]
    (test/are [status route route-params]
      (let [endpoint (util/endpoint-url *system* route route-params)
            response (http/get endpoint {:follow-redirects false})]
        (= (:status response)
           status))
      200 :index nil
      200 :browse/all-items nil
      red :user-settings nil
      red :store-dashboard {:store-id 123})))

(deftest test-authed-http
  (let [red (:found response/redirect-status-codes)
        db (util/system-db *system*)
        user-id (db/one-with db {:where   '[[?e :user/email ?email]]
                                 :symbols {'?email user-email}})
        store-id (db/one-with db {:where   '[[?e :store/owners ?owners]
                                             [?owners :store.owner/user ?user]]
                                  :symbols {'?user user-id}})]
    (util/with-auth
      *system* user-email
      (test/are [status route route-params]
        (let [endpoint (util/endpoint-url *system* route route-params)
              response (http/get endpoint
                                 {:follow-redirects false})]

          (= status (:status response)))

        200 :index nil
        200 :user-settings nil
        200 :store-dashboard {:store-id store-id}
        red :store-dashboard {:store-id (dec store-id)}))))

(deftest test-authed-no-locality-should-not-set-cookie-http
  (let [red (:found response/redirect-status-codes)
        db (util/system-db *system*)]
    (util/with-auth
      *system* user-email
      (test/are [_ route route-params]
        (let [endpoint (util/endpoint-url *system* route route-params)
              response (http/get endpoint {:follow-redirects false})
              cookie-locality (auth/cookie-locality response)]
          (= nil (:sulo-locality/path cookie-locality)))
        200 :index nil
        200 :index nil
        200 :user-settings nil))))
