(ns scape.emitter
  (:require [datomic.api :refer [tempid]]))

(defn- id []
  (tempid :db.part/user))

(defn- emit-common
  [entity-id {:keys [env op form]}]
  [[:db/add entity-id :ast/op (keyword "ast.op" (name op))]
   [:db/add entity-id :ast/line (or (:line env) -1)]
   [:db/add entity-id :ast/form (pr-str form)]])

(defmulti emit :op)

(defmethod emit :if
  [{:keys [test then else] :as ast}]
  (let [entity-id (id)
        {test-id :entity-id
         test-tx :transaction} (emit test)
        {then-id :entity-id
         then-tx :transaction} (emit then)
        {else-id :entity-id
         else-tx :transaction} (emit else)]
    {:entity-id entity-id
     :transaction (concat (emit-common entity-id ast)
                          [[:db/add entity-id :ast.if/test test-id]
                           [:db/add entity-id :ast.if/then then-id]
                           [:db/add entity-id :ast.if/else else-id]]
                          test-tx
                          then-tx
                          else-tx)}))
    
(defmethod emit :def
  [{:keys [name init doc] :as ast}]
  (let [entity-id (id)
        {init-id :entity-id
         init-tx :transaction} (when init (emit init))]
    {:entity-id entity-id
     :transaction (concat (emit-common entity-id ast)
                          (when init
                            [[:db/add entity-id :ast.def/init init-id]])
                          (when doc
                            [[:db/add entity-id :ast.def/doc doc]])
                          [[:db/add entity-id :ast/name (str name)]]
                          init-tx)}))

(defn- local? [var-node]
  (let [var (:form var-node)]
    (contains? (-> var-node :env :locals) var)))

(defmethod emit :var
  [ast]
  (let [entity-id (id)]
    {:entity-id entity-id
     :transaction (concat (emit-common entity-id ast)
                          [[:db/add entity-id :ast.var/local (local? ast)]
                           [:db/add entity-id :ast/name (-> ast :info :name str)]]
                          (when-not (local? ast)
                            [[:db/add entity-id :ast/ns (-> ast :info :ns str)]]))}))

;; TODO: numbered statements or linked list?
(defn block-transaction [eid {:keys [statements ret]}]
  (let [stmnt-tx-data (map emit statements)
        stmnt-ids (map :entity-id stmnt-tx-data)
        stmnt-txs (map :transaction stmnt-tx-data)
        {ret-id :entity-id
         ret-tx :transaction} (map emit ret)]
    (concat (map #(vector :db/add eid :ast/statement %) stmnt-ids)
            stmnt-txs
            [[:db/add eid :ast/ret ret-id]]
            ret-tx)))

;;(defmethod emit :fn
;;  [ast]
;;  ..)

(defmethod emit :constant
  [{:keys [form] :as ast}]
  (let [entity-id (id)
        form-type (pr-str (type form))
        form-str (pr-str form)]
    {:entity-id entity-id
     :transaction (concat (emit-common entity-id ast)
                          [[:db/add entity-id :ast.constant/type form-type]])}))

(defmethod emit :default
  [{:keys [op children form] :as ast}]
  (let [entity-id (id)
        txdata (map emit children)
        tx (mapcat :transaction txdata)
        child-ids (map #(vector :db/add entity-id :ast/child (:entity-id %))
                       txdata)]
    {:entity-id entity-id
     :transaction (concat (emit-common entity-id ast)
                          child-ids
                          tx)}))

(defn emit-transaction-data [ast]
  (-> ast emit :transaction vec))
