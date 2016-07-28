(ns git-internal.core-test
  (:require [clojure.test :refer :all]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [git-internal.core :refer :all]
            [gloss.core :refer :all]
            [gloss.io :refer :all]))

(defspec sha1hex-len-spec
  (prop/for-all [v gen/bytes]
                (= 40 (.length (sha1hex v)))))

(deftest blob-digest-test
  (testing "sample"
    (are [obj-id str] (= obj-id (-> str .getBytes blob-digest hex-str))
      "d670460b4b4aece5915caf5c68d12f560a9fe3e4" "test content\n"
      "bd9dbf5aae1a3862dd1526723246b20206e5fc37" "what is up, doc?")))

(defspec deflate-test
  (prop/for-all [v gen/bytes]
                (= (seq v) (seq (inflate (deflate v))))))

(defspec tree->bytes-spec
  (prop/for-all [tree (gen/not-empty
                       (gen/vector (gen/hash-map
                                    :mode (gen/elements ["100644" "100755" "120000" "040000"])
                                    :filename (gen/not-empty gen/string-alphanumeric)
                                    :oid (gen/fmap byte-array (gen/vector gen/byte 20)))))]
                (let [oid->seq (fn [e] (update e :oid seq))]
                  (= (sort-by :filename (map oid->seq tree))
                     (map oid->seq (bytes->tree (tree->bytes tree)))))))

(defspec encode-all-spec-ok
  (prop/for-all [mode (gen/elements ["100644" "100755" "120000" "040000"])
                 frame (gen/return (compile-frame {:mode (string :utf-8 :length 6 :suffix " ")}))]
                (= 0 (reduce + (map (memfn position) (encode-all frame [{:mode mode}]))))))
