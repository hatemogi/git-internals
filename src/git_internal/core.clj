(ns git-internal.core
  (:require [clojure.java.io :as io]
            [gloss.core :refer :all]
            [gloss.io :refer :all])
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

(defcodec object-header
  (ordered-map
   :type (string :utf-8 :delimiters [" "])
   :len (string-integer :ascii :delimiters ["\0"])))

(defn- encode-to-bytes [frame vals]
  (let [buf (contiguous (encode-all frame vals))
        res (byte-array (.limit buf))]
    (.get buf res)
    res))

(defn -blob-digest [bytes]
  (let [header (str "blob " (alength bytes) "\0")]
    (-> (sha1update header)
        (sha1update bytes)
        sha1final)))

(defn blob-digest [bytes]
  (let [header (encode-to-bytes object-header
                                [{:type "blob" :len (alength bytes)}])]
    (-> (sha1update header)
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

(defrecord TreeEntry [mode filename oid])
(defcodec tree-entry (ordered-map :mode     (string :utf-8 :length 6 :suffix " ") ;;
                                  :filename (string :utf-8 :delimiters ["\0"]) ;;
                                  :oid      (finite-block 20)))

(defcodec str-codec (compile-frame {:mode  (string :utf-8 :length 6 :suffix " ")}))

(defn -tree->bytes [tree]
  (stream->bytes [out]
    (doseq [{:keys [mode type oid filename]} (sort-by :filename tree)]
      (doto out
        (.write (.getBytes (name mode)))
        (.write 32)
        (.write (.getBytes filename))
        (.write 0)
        (.write (raw-bytes oid))))))

(defn tree->bytes [tree]
  (encode-to-bytes tree-entry (sort-by :filename tree)))

(defn- read-null-terminated-str [in]
  (String. (stream->bytes [out]
             (loop [b (.read in)]
               (cond
                 (neg? b)  (throw (java.io.EOFException.))
                 (zero? b) :done
                 :else     (do (.write out b)
                               (recur (.read in))))))))

(defn -bytes->tree [bytes]
  (let [mode (byte-array 6)
        oid  (byte-array 20)]
    (with-open [in (ByteArrayInputStream. bytes)]
      (loop [len (alength bytes) res ()]
        (if (pos? len)
          (do
            (.read in mode 0 6)
            (assert (= 32 (.read in)) "it must be a space between mode and filename")
            (let [mode     (keyword (String. mode))
                  filename (read-null-terminated-str in)
                  _        (.read in oid 0 20)
                  oid      (aclone oid)]
              (recur (- len 6 1 (alength (.getBytes filename)) 1 20)
                     (conj res (TreeEntry. mode oid filename)))))
          (reverse res))))))

(defn bytes->tree [bytes]
  (decode-all tree-entry bytes))
