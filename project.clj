(defproject chapter-6 "0.1.0-SNAPSHOT"
  :description "Resources and examples for Professional Clojure, Chapter 6: Datomic"
  :url "http://www.wiley.com/WileyCDA/WileyTitle/productCd-1119267277.html"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.datomic/datomic-free "0.9.5344"]
                 [crypto-password "0.1.3"]]
  :profiles {:dev {:source-paths ["dev"]}})
