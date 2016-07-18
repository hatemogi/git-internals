(ns git-internal.core
  (:require [clojure.java.io :as io])
  (:import java.security.MessageDigest
           java.math.BigInteger))

(defn sha1hex [bytes]
  (let [digest (.digest (doto (MessageDigest/getInstance "SHA-1")
                          (.update bytes)))]
    (format "%040x" (BigInteger. 1 digest))))

(defn hash-object [bytes]
  )
