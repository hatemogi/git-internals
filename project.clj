(defproject git-internal "0.1.0-SNAPSHOT"
  :description "a toy project for studying Git Internals"
  :url "https://github.com/hatemogi/git-internal"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [gloss "0.2.6"]
                 [instaparse "1.4.2"]]
  :main ^:skip-aot git-internal.core
  :target-path "target/%s"
  :plugins [[lein-codox "0.9.5"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}})
