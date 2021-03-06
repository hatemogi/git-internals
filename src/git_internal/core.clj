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

(defn- heaps->bytes [hseq]
  (let [buf (contiguous hseq)
        res (byte-array (.limit buf))]
    (.get buf res)
    res))

(def ^:private encode-to-bytes
  (comp heaps->bytes encode-all))

(defn blob-digest [bytes]
  (let [header (encode-to-bytes object-header
                                [{:type "blob" :len (alength bytes)}])]
    (-> (sha1update header)
        (sha1update bytes)
        sha1final)))

(def ^:private test-oid (blob-digest (.getBytes "sample-oid")))

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
(defcodec tree-codec (ordered-map :mode     (string :utf-8 :length 6 :suffix " ")
                                  :filename (string :utf-8 :delimiters ["\0"])
                                  :oid      (finite-block 20)))

(defn tree->bytes [tree]
  (encode-to-bytes tree-codec (sort-by :filename tree)))

(defn bytes->tree [bytes]
  (map (fn [e] (update e :oid heaps->bytes))
       (decode-all tree-codec bytes)))

(defn- ident->str [ident]
  )

(defn commit->bytes [commit]
  (stream->bytes [out]
    (let [puts (fn [& s]
                 (if (seq s) (.write out (.getBytes (apply str s))))
                 (.write out 10))]
      (puts "tree " (hex-str (:tree commit)))
      (doseq [parent (:parents commit)]
        (puts "parent " (hex-str parent)))
      (puts "author " (:author commit))
      (puts "committer " (ident->str (:committer commit)))
      (puts "encoding " (ident->str (:encoding commit)))
      (puts)
      (.write out (.getBytes (:message commit))))))
