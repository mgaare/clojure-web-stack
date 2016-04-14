(ns dev
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer (pprint)]
            [datomic.api :as d])
  (:import datomic.Util))

(def dev-db-uri
  "datomic:mem://dev-db")

(def schema
  (io/resource "schema.edn"))

(def example-data
  (io/resource "example-data.edn"))

(defn read-txs
  [tx-resource]
  (with-open [tf (io/reader tx-resource)]
    (Util/readAll tf)))

(defn transact-all
  ([conn txs]
   (transact-all conn txs nil))
  ([conn txs res]
   (if (seq txs)
     (transact-all conn (rest txs) @(d/transact conn (first txs)))
     res)))

(defn initialize-db
  "Creates db, connects, transacts schema and example data, returns conn."
  []
  (d/create-database dev-db-uri)
  (let [conn (d/connect dev-db-uri)]
    (transact-all conn (read-txs schema))
    (transact-all conn (read-txs example-data))
    conn))

(defonce conn nil)

(defn go
  []
  (alter-var-root #'conn (constantly (initialize-db))))

(defn stop
  []
  (alter-var-root #'conn
                  (fn [c] (when c (d/release c)))))
