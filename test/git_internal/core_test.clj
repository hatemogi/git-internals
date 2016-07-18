(ns git-internal.core-test
  (:require [clojure.test :refer :all]
            [clojure.test.check
                [clojure-test :refer [defspec]]
                [generators :as gen]
                [properties :as prop]]
            [git-internal.core :refer :all]))

(defspec sha1hex-len-test 100
  (prop/for-all [v gen/string]
                (= 40 (.length (sha1hex (.getBytes v))))))

(deftest blob-digest-test
  (testing "sample"
    (are [obj-id str] (= obj-id (blob-digest (.getBytes str)))
      "d670460b4b4aece5915caf5c68d12f560a9fe3e4" "test content\n"
      "bd9dbf5aae1a3862dd1526723246b20206e5fc37" "what is up, doc?")))
