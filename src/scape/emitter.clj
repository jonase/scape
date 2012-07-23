(ns scape.emitter
  (:require [datomic.api :refer [tempid]]))

(defn- id []
  (tempid :db.part/user))

(defn- emit-common
  [parent-id entity-id {:keys [env op form] :as ast}]
  (concat [[:db/add entity-id :ast/op op]
           [:db/add entity-id :ast/ns (-> env :ns :name)]
           [:db/add entity-id :ast/form (pr-str form)]]
          (when parent-id
            [[:db/add parent-id :ast/child entity-id]])
          (when-let [line (:line env)]
            [[:db/add entity-id :ast/line line]])))

(defmulti emit
  (fn [parent-id expr-obj]
    (:op expr-obj)))

(defmethod emit :if
  [parent-id {:keys [test then else] :as ast}]
  (let [entity-id (id)
        {test-id :entity-id
         test-tx :transaction} (emit entity-id test)
        {then-id :entity-id
         then-tx :transaction} (emit entity-id then)
        {else-id :entity-id
         else-tx :transaction} (emit entity-id else)]
    {:entity-id entity-id
     :transaction (concat (emit-common parent-id entity-id ast)
                          [[:db/add entity-id :ast.if/test test-id]
                           [:db/add entity-id :ast.if/then then-id]
                           [:db/add entity-id :ast.if/else else-id]]
                          test-tx
                          then-tx
                          else-tx)}))

(defmethod emit :throw
  [parent-id {throw-expr :throw :as ast}]
  (let [entity-id (id)
        {throw-id :entity-id
         throw-tx :transaction} (emit entity-id throw-expr)]
    {:entity-id entity-id
     :transaction (concat (emit-common parent-id entity-id ast)
                          [[:db/add entity-id :ast.throw/expr throw-id]]
                          throw-tx)}))

(defmethod emit :def
  [parent-id {:keys [name init doc] :as ast}]
  (let [entity-id (id)
        {init-id :entity-id
         init-tx :transaction} (when init (emit entity-id init))]
    {:entity-id entity-id
     :transaction (concat (emit-common parent-id entity-id ast)
                          (when init
                            [[:db/add entity-id :ast/init init-id]])
                          (when doc
                            [[:db/add entity-id :db/doc doc]])
                          [[:db/add entity-id :db/ident (keyword name)]]
                          init-tx)}))

(defn- local? [var-node]
  (boolean (or (-> var-node :info :local)
               (-> var-node :info :name namespace not))))

(defmethod emit :var
  [parent-id ast]
  (let [entity-id (id)]
    {:entity-id entity-id
     :transaction (concat (emit-common parent-id entity-id ast)
                          [[:db/add entity-id :ast.var/local (local? ast)]
                           [:db/add entity-id :ast/name (-> ast :info :name keyword)]]
                          ;; Drop, or rename to e.g :ast.var/ns
                          #_(when-not (local? ast)
                            [[:db/add entity-id :ast/ns
                              (or (-> ast :info :ns keyword)
                                  :bad-ns)]]))})) ;; NOTE: due to possible "buggy" cljs code
                                                  ;; e.g. a.state instead of (.-state a)

;; TODO: numbered statements or linked list?
;; Note: when parent-id == entity-id this block is a 'direct child', e.g.,
;; :do or :let. Otherwise e.g., :fn
(defn emit-block [parent-id eid {:keys [statements ret]}]
  (let [stmnt-tx-data (map emit statements)
        stmnt-ids (map :entity-id stmnt-tx-data)
        stmnt-txs (mapcat :transaction stmnt-tx-data)
        {ret-id :entity-id
         ret-tx :transaction} (emit ret)]
    (concat (map #(vector :db/add eid :ast/statement %) stmnt-ids)
            stmnt-txs
            [[:db/add eid :ast/ret ret-id]]
            ret-tx)))

(defn emit-fn-method
  [parent-id eid {:keys [variadic max-fixed-arity] :as method}]
  (let [method-id (id)]
    (concat [[:db/add eid :ast.fn/method method-id]
             [:db/add method-id :ast.fn/variadic variadic]
             [:db/add method-id :ast.fn/fixed-arity max-fixed-arity]]
            (emit-block parent-id method-id method))))

(defmethod emit :fn
  [parent-id {:keys [methods] :as ast}]
  (let [entity-id (id)
        method-txs (mapcat #(emit-fn-method parent-id entity-id %) methods)]
    {:entity-id entity-id
     :transaction (concat (emit-common parent-id entity-id ast)
                          method-txs)}))
    
(defmethod emit :do
  [parent-id ast]
  (let [entity-id (id)]
    {:entity-id entity-id
     :transaction (concat (emit-common parent-id entity-id ast)
                          (emit-block entity-id entity-id ast))}))

(defmethod emit :constant
  [parent-id {:keys [form] :as ast}]
  (let [entity-id (id)
        form-type (pr-str (type form))]
    {:entity-id entity-id
     :transaction (concat (emit-common parent-id entity-id ast)
                          [[:db/add entity-id :ast.constant/type form-type]])}))

(defn- emit-binding [parent-id eid {:keys [name init]}]
  (let [binding-id (id)
        {init-id :entity-id
         init-tx :transaction} (emit parent-id init)]
    (concat [[:db/add eid :ast.let/binding binding-id]
             [:db/add binding-id :ast/name (keyword name)]
             [:db/add binding-id :ast/init init-id]]
            init-tx)))

(defn- emit-bindings [parent-id eid bindings]
  (mapcat #(emit-binding parent-id eid %) bindings))

(defmethod emit :let
  [parent-id {:keys [loop bindings] :as ast}]
  (let [entity-id (id)]
    {:entity-id entity-id
     :transaction (concat (emit-common parent-id entity-id ast)
                          (emit-bindings entity-id entity-id bindings)
                          (emit-block entity-id entity-id ast)
                          [[:db/add entity-id :ast.let/loop loop]])}))

(defmethod emit :invoke
  [parent-id {:keys [f args] :as ast}]
  (let [entity-id (id)
        {fid :entity-id
         ftx :transaction} (emit entity-id f)
        args-tx-data (map #(emit entity-id %) args)]
    {:entity-id entity-id
     :transaction (concat (emit-common parent-id entity-id ast)
                          [[:db/add entity-id :ast.invoke/f fid]]
                          ftx
                          (map #(vector :db/add entity-id :ast/arg (:entity-id %))
                               args-tx-data)
                          (mapcat :transaction args-tx-data))}))

(defmethod emit :recur
  [parent-id {:keys [exprs] :as ast}]
  (let [entity-id (id)
        args-tx-data (map #(emit entity-id %) exprs)]
    {:entity-id entity-id
     :transaction (concat (emit-common parent-id entity-id ast)
                          (map #(vector :db/add entity-id :ast/arg (:entity-id %))
                               args-tx-data)
                          (mapcat :transaction args-tx-data))}))

(defmethod emit :default
  [parent-id {:keys [op children form] :as ast}]
  (let [entity-id (id)
        txdata (map #(emit entity-id %) children)
        tx (mapcat :transaction txdata)]
    {:entity-id entity-id
     :transaction (concat (emit-common parent-id entity-id ast)
                          tx)}))

(defn emit-transaction-data [ast]
  (let [{eid :entity-id
         tx :transaction} (emit ast)]
    (cons [:db/add eid :ast/top-level true]
          tx)))
