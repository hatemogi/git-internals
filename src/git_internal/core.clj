(ns git-internal.core
  (:require [clojure.java.io :as io])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           java.math.BigInteger
           java.security.MessageDigest
           [java.util.zip DeflaterOutputStream InflaterInputStream]))

(defprotocol ObjectId
  (raw-bytes [this])
  (hex-str [this]))

(extend-protocol ObjectId
  (class (byte-array 0))
  (raw-bytes [this] this)
  (hex-str [this] (format "%040x" (BigInteger. 1 this))))

(defn- sha1update
  ([bytes] (sha1update (MessageDigest/getInstance "SHA-1") bytes))
  ([digest bytes] (.update digest bytes) digest))

(defn- sha1final [digest]
  (hex-str (.digest digest)))

(def sha1hex (comp sha1final sha1update))

(defn blob-digest [bytes]
  (let [header (str "blob " (alength bytes) "\0")]
    (-> (sha1update (.getBytes header))
        (sha1update bytes)
        sha1final)))

(defmacro stream->bytes
  {:style/indent 1}
  [[out] & body]
  `(with-open [~out (ByteArrayOutputStream.)]
     ~@body
     (.toByteArray ~out)))

(defn deflate [bytes]
  (stream->bytes [out]
    (with-open [stream (DeflaterOutputStream. out)]
      (.write stream bytes)
      (.finish stream))))

(defn inflate [bytes]
  (stream->bytes [out]
    (with-open [in (InflaterInputStream. (ByteArrayInputStream. bytes))]
      (io/copy in out))))

(defrecord TreeEntry [mode type oid filename])

(defn tree-entries->bytes [entries]
  (stream->bytes [out]
    (doseq [{:keys [mode type oid filename]} entries]
      (doto out
        (.write (.getBytes (name mode)))
        (.write 32)
        (.write (.getBytes filename))
        (.write 0)
        (.write (raw-bytes oid))))))
