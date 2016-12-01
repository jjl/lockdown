(ns irresponsible.lockdown-test
  (:require [irresponsible.lockdown :as l]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.test :as t])
  (:import [java.lang SecurityException]))

(t/deftest sec-ex
  (t/is (= ::throw
           (try
             (l/sec-ex "foo")
             (catch SecurityException e
               (t/is (= "foo" (.getMessage e)))
               ::throw)))))

(t/deftest full
  (l/lock-down
   (l/sec-man p (l/sec-ex (.getName p))
              true)))
;              l/sec-ex (try (= (.getName p) "foo:1234"))) true)))
;  (try
;    (.checkPackageDefinition (System/getSecurityManager) "foo")
;    (catch SecurityException e
;      (t/is (= "true" (.getMessage e)))
;      ::throw)))

