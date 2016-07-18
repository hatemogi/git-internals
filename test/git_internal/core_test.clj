(ns git-internal.core-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [git-internal.core :refer :all]))

(defspec sha1hex-test 100
  (prop/for-all [v gen/string]
                (= 40 (.length (sha1hex (.getBytes v))))))
