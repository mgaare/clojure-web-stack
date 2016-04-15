(ns book)

;; REPL examples from the chapter. It's intended that each expression
;; be evaluated, in order, at the REPL.

(require 'dev)

(in-ns 'dev)

(go)

(def db (d/db conn))

(d/attribute db :task/title)

(d/basis-t db)

(d/t->tx 1021)

(first (d/datoms db :aevt))

(first (d/datoms db :aevt :db/doc :task/title))

(d/entid db :task/title)

(d/ident db 67)

(d/entity db [:user/login "janed"])

(def jane (d/entity db [:user/login "janed"]))

(keys jane)

(:user/login jane)

(vals jane)

(into {} jane)

(get-in jane [:user/account :account/type])

(d/touch (:user/account jane))

(d/q '[:find ?login :in $ ?email
       :where [?user :user/email ?email]
       [?user :user/login ?login]]
     db "everywoman123@gmail.com")

(d/q '[:find ?login :in $ [?email ...]
       :where [?user :user/email ?email]
       [?user :user/login ?login]]
     db
     ["everywoman123@gmail.com" "john.doe@nowhere.com" "postmaster@google.com"])

(d/q '[:find (pull ?user [:user/login
                          {:user/account [{:account/type [:db/ident]}]}])
       :in $ ?email
       :where [?user :user/email ?email]]
     db "everywoman123@gmail.com")

(pprint @(d/transact conn [{:db/id (d/tempid :db.part/user)
                            :task/title "Hello world"}]))

(pprint @(d/transact conn
                     [[:db/add 17592186045454
                       :task/title "Hello world"]]))

(def db1 (:db-after @(d/transact conn [{:db/id (d/tempid :db.part/user)
                                        :task/issue-id "Hello"}])))

(def db2 (:db-after @(d/transact conn [{:db/id (d/tempid :db.part/user)
                                        :task/issue-id "Hello"
                                        :task/description "First description"}])))

(def db3 (:db-after @(d/transact conn [{:db/id (d/tempid :db.part/user)
                                        :task/issue-id "Hello"
                                        :task/description "Second description"}])))

(def now-db (d/db conn)) ;; get the most current db

(:task/description (d/entity now-db [:task/issue-id "Hello"]))

(:task/description (d/entity (d/as-of now-db (d/basis-t db2))
                             [:task/issue-id "Hello"]))

(def description-query '[:find ?i . :in $ ?desc
                         :where [?i :task/description ?desc]])

(d/q description-query now-db "Second description")

(d/q description-query (d/as-of now-db (d/basis-t db2)) "First description")

(def future-db (-> now-db
                   (d/with [{:db/id (d/tempid :db.part/user)
                             :task/issue-id "Hello"
                             :task/description "Third description"}])
                   :db-after
                   (d/with [{:db/id (d/tempid :db.part/user)
                             :task/issue-id "Hello"
                             :task/title "Hello world"}])
                   :db-after))

(d/touch (d/entity future-db [:task/issue-id "Hello"]))
