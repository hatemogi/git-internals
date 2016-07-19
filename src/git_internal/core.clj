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
  (.digest digest))

(def sha1digest (comp sha1final sha1update))
(def sha1hex (comp hex-str sha1digest))

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

(defn tree->bytes [tree]
  (stream->bytes [out]
    (doseq [{:keys [mode type oid filename]} (sort-by :filename tree)]
      (doto out
        (.write (.getBytes (name mode)))
        (.write 32)
        (.write (.getBytes filename))
        (.write 0)
        (.write (raw-bytes oid))))))

(defn- read-null-terminated-str [in]
  (String. (stream->bytes [out]
             (loop [b (.read in)]
               (cond
                 (neg? b)  (throw (java.io.EOFException.))
                 (zero? b) :done
                 :else     (do (.write out b)
                               (recur (.read in))))))))

(defn bytes->tree [bytes]
  (let [mode (byte-array 6)
        oid  (byte-array 20)]
    (with-open [in (ByteArrayInputStream. bytes)]
      (loop [len (alength bytes) res ()]
        (if (pos? len)
          (do
            (.read in mode 0 6)
            (assert (= 32 (.read in)) "it must be a space between mode and filename")
            (let [mode     (keyword (String. mode))
                  type     (case mode
                             :100644 :blob
                             :100755 :blob
                             :040000 :tree
                             (throw (java.io.InvalidObjectException. (str "Unexpected mode: " mode))))
                  filename (read-null-terminated-str in)
                  _        (.read in oid 0 20)
                  oid      (aclone oid)]
              (recur (- len 6 1 (alength (.getBytes filename)) 1 20)
                     (conj res (TreeEntry. mode type oid filename)))))
          (reverse res))))))
