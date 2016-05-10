(ns clojure-web-stack.account
  (:require [datomic.api :as d]))

(defn calculate-balance
  "Takes a user or account entity, and recalculates the accounts's
   current balance by summing the charges, payments, and adjustments."
  [user-or-account]
  (->> (or (:account/transaction user-or-account)
           (get-in user-or-account [:user/account :account/transaction]))
       (map :transaction/amount)
       (reduce +)))

(defn add-transaction
  "Takes a conn, user or account entity, transaction type (ident of
   the enum), and amount, and adds the transaction to the user's
   account. Returns the transaction data."
  [conn user-or-account trans-type amount]
  (let [account (or (:user/account user-or-account) user-or-account)
        amount (bigdec amount)
        charge-tx [{:db/id (d/tempid :db.part/account)
                    :transaction/type trans-type
                    :transaction/amount amount
                    :account/_transaction (:db/id account)}
                   [:account/update-balance (:db/id account) amount]]]
    @(d/transact conn charge-tx)))

(defn add-charge
  "Adds a charge with add-transaction."
  [conn user-or-account amount]
  (add-transaction conn user-or-account :transaction.type/charge amount))

(defn add-payment
  "Adds a payment with add-transaction."
  [conn user-or-account amount]
  (add-transaction conn user-or-account :transaction.type/payment amount))

(defn add-adjustment
  "Adds an adjustment with add-transaction."
  [conn user-or-account amount]
  (add-transaction conn user-or-account :transaction.type/adjustment amount))
