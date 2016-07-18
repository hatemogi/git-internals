(ns git-internal.core
  (:require [clojure.java.io :as io])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           java.math.BigInteger
           java.security.MessageDigest
           [java.util.zip DeflaterOutputStream InflaterInputStream]))

(defn- sha1update
  ([bytes] (sha1update (MessageDigest/getInstance "SHA-1") bytes))
  ([digest bytes] (.update digest bytes) digest))

(defn- sha1final [digest]
  (format "%040x" (BigInteger. 1 (.digest digest))))

(def sha1hex (comp sha1final sha1update))

(defn blob-digest [bytes]
  (let [header (.getBytes (str "blob " (alength bytes) "\0"))]
    (-> (sha1update header)
        (sha1update bytes)
        sha1final)))

(defmacro stream-to-bytes
  {:style/indent 1}
  [[out] & body]
  `(with-open [~out (ByteArrayOutputStream.)]
     ~@body
     (.toByteArray ~out)))

(defn deflate [bytes]
  (stream-to-bytes [out]
    (with-open [stream (DeflaterOutputStream. out)]
      (.write stream bytes)
      (.finish stream))))

(defn inflate [bytes]
  (stream-to-bytes [out]
    (with-open [in (InflaterInputStream. (ByteArrayInputStream. bytes))]
      (io/copy in out))))
