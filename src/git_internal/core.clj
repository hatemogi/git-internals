(ns git-internal.core
  (:import java.math.BigInteger
           java.security.MessageDigest))

(defn sha1update
  ([bytes] (sha1update (MessageDigest/getInstance "SHA-1") bytes))
  ([digest bytes] (.update digest bytes) digest))

(defn sha1final [digest]
  (format "%040x" (BigInteger. 1 (.digest digest))))

(def sha1hex (comp sha1final sha1update))

(defn blob-digest [bytes]
  (let [header (.getBytes (str "blob " (alength bytes) "\0"))]
    (-> (sha1update header)
        (sha1update bytes)
        sha1final)))
