(ns eponai.common.parser.mutate
 (:require [clojure.set :refer [rename-keys]]
           [eponai.common.format :as format]
           [eponai.common.validate :as validate]
           [eponai.common.transact :as transact]
   #?(:clj
           [eponai.server.api :as api])

   #?(:clj [clojure.core.async :refer [go >! chan]])
  #?(:clj [eponai.server.datomic.pull :as server.pull])
  #?(:cljs [datascript.core :as d])
  #?(:cljs [om.next :as om])))

(defmulti mutate (fn [_ k _] k))

;; -------- Badly scoped mutations. TODO:(FIX THIS)

#?(:cljs
   (defmethod mutate 'datascript/transact
    [{:keys [unsafe-conn]} _ {:keys [txs]}]
    {:action #(d/transact! unsafe-conn txs)}))

#?(:cljs
   (defn cas! [component id key old-value new-value]
    (om/transact! component
                  `[(datascript/transact
                     {:txs [[:db.fn/cas ~id ~key ~old-value ~new-value]]})])))

;; -------- Remote mutations

(defn- transaction-create [{:keys [unsafe-conn db auth]} k {:keys [input-tags] :as params}]
  (fn []
    (when-not (= (frequencies (set input-tags))
                 (frequencies input-tags))
      (throw (ex-info "Illegal argument :input-tags. Each tag must be unique."
                      {:input-tags input-tags
                       :mutate     k
                       :params     params})))
    (let [renames {:input-title       :transaction/name
                   :input-amount      :transaction/amount
                   :input-description :transaction/details
                   :input-date        :transaction/date
                   :input-tags        :transaction/tags
                   :input-currency    :transaction/currency
                   :input-created-at  :transaction/created-at
                   :input-uuid        :transaction/uuid}
          user-tx (rename-keys params renames)
          #?@(:clj [user-tx (assoc user-tx :transaction/budget
                                           (:budget/uuid (server.pull/budget db (:username auth))))
                    currency-chan (chan 1)])
          _ (validate/valid-user-transaction? user-tx)
          db-tx (format/user-transaction->db-entity user-tx)]
      (transact/transact unsafe-conn [db-tx])
      #?(:clj (go (>! currency-chan (:transaction/date user-tx))))
      #?(:clj {:currency-chan currency-chan}
         :cljs nil))))

(defmethod mutate 'transaction/create
  [env k params]
  {:action (transaction-create env k params)
   #?@(:cljs [:remote true])})

(defmethod mutate 'email/verify
  [{:keys [unsafe-conn]} _ {:keys [uuid]}]
  (println "Verification verify: " uuid)
  #?(:cljs {:remote true}
     :clj  {:action (fn []
                      (api/verify unsafe-conn uuid)
                      uuid)}))
