(defproject clojure-web-stack "0.1.0-SNAPSHOT"
  :description "Example project used to talk about the Clojure web stack."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.datomic/datomic-free "0.9.5344"]
                 [crypto-password "0.1.3"]

                 ;; web libs
                 [ring "1.4.0"]
                 [compojure "1.5.0"]
                 [liberator "0.14.1"]
                 ]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [ring/ring-mock "0.3.0"]]}})
