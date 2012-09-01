(ns scape.emitter
  (:require [datomic.api :refer [tempid]]))

(defn assoc-id [expr-obj]
  (assoc expr-obj :db/id (tempid :db.part/user)))

(defn emit-common [expr-obj]
  {:db/id (:db/id expr-obj)
   :ast/op (:op expr-obj)               ;; Could be derived
   :ast/ns (-> expr-obj :env :ns :name) ;; Could be derived from top level form
   :ast/form (-> expr-obj :form pr-str) ;; Record forms only on constants?
   :ast/line (-> expr-obj :env :line)}) ;; 

(defmulti emit :op)

(defmethod emit :if [expr-obj]
  (let [test (assoc-id (:test expr-obj))
        then (assoc-id (:then expr-obj))
        else (assoc-id (:else expr-obj))]
    (concat [(assoc (emit-common expr-obj)
               :ast.if/test (:db/id test)
               :ast.if/then (:db/id then)
               :ast.if/else (:db/id else))]
            (emit test)
            (emit then)
            (emit else))))

(defn emit-var [expr-obj]
  [(assoc (emit-common expr-obj)
     :ast.var/name (-> expr-obj :info :name name keyword)
     :ast.var/ns (-> expr-obj :info :name namespace keyword)
     :ast.var/ns-qualified-name (-> expr-obj :info :name keyword))])

(defn emit-local [expr-obj]
  [(assoc (emit-common expr-obj)
     :ast.local/name (-> expr-obj :info :name keyword))])

(defmethod emit :var [expr-obj]
  (if (-> expr-obj :info :name namespace)
    (emit-var expr-obj)
    (emit-local expr-obj)))
  
(defmethod emit :def [{:keys [name doc init] :as expr-obj}]
  (let [init (when init (assoc-id (:init expr-obj)))]
    (concat [(merge (emit-common expr-obj)
                    {:ast.def/name (keyword name)}
                    (when init {:ast.def/init (:db/id init)})
                    (when doc  {:ast.def/doc doc}))]
             (when init (emit init)))))

;; TODO: indexed statements?
(defn emit-block [statements return]
  (let [statements-tx (mapcat emit statements)
        return-tx (emit return)]
    (concat statements-tx
            return-tx)))

;; TODO: arity etc.
(defn emit-fn-method [fn-method]
  (let [statements (map assoc-id (:statements fn-method))
        return (assoc-id (:ret fn-method))]
    (concat [{:db/id (:db/id fn-method)
              :ast.fn.method/statement (map :db/id statements)
              :ast.fn.method/return (:db/id return)}]
            (emit-block statements return))))

(defmethod emit :fn [expr-obj]
  (let [methods (map assoc-id (:methods expr-obj))]
    (concat [(assoc (emit-common expr-obj)
               :ast.fn/method (map :db/id methods))]
            (mapcat emit-fn-method methods))))

(defmethod emit :do [expr-obj]
  (let [statements (map assoc-id (:statements expr-obj))
        return (assoc-id (:ret expr-obj))]
    (concat [(assoc (emit-common expr-obj)
               :ast.do/statement (map :db/id statements)
               :ast.do/return (:db/id return))]
            (emit-block statements return))))

(defmethod emit :constant [expr-obj]
  [(emit-common expr-obj)])

(defn emit-binding [binding]
  (let [init (assoc-id (:init binding))]
    (concat [{:db/id (:db/id binding)
              :ast.let.binding/name (-> binding :name keyword)
              :ast.let.binding/init (:db/id init)}]
            (emit init))))

(defmethod emit :let [expr-obj]
  (let [bindings (map assoc-id (:bindings expr-obj))
        statements (map assoc-id (:statements expr-obj))
        return (assoc-id (:ret expr-obj))]
    (concat [(assoc (emit-common expr-obj)
               :ast.let/loop (:loop expr-obj)
               :ast.let/binding (map :db/id bindings)
               :ast.let/statement (map :db/id statements)
               :ast.let/return (:db/id return))]
            (mapcat emit-binding bindings)
            (emit-block statements return))))

(defmethod emit :invoke [expr-obj]
  (let [f (assoc-id (:f expr-obj))
        args (map assoc-id (:args expr-obj))]
    (concat [(assoc (emit-common expr-obj)
               :ast.invoke/f (:db/id f)
               :ast.invoke/arg (map :db/id args))]
            (emit f)
            (mapcat emit args))))

(defmethod emit :recur [expr-obj]
  (let [args (map assoc-id (:exprs expr-obj))]
    (concat [(assoc (emit-common expr-obj)
               :ast.recur/arg (map :db/id args))]
            (mapcat emit args))))

(defmethod emit :deftype* [expr-obj]
  (let [t (:t expr-obj)]
    [(assoc (emit-common expr-obj)
       :ast.deftype*/name (name t)
       :ast.deftype*/ns (namespace t)
       :ast.deftype*/ns-qualified-name t
       :ast.deftype*/field (:fields expr-obj))]))

(defmethod emit :defrecord* [expr-obj]
  (let [t (:t expr-obj)]
    [(assoc (emit-common expr-obj)
       :ast.defrecord*/name (name t)
       :ast.defrecord*/ns (namespace t)
       :ast.defrecord*/ns-qualified-name t
       :ast.defrecord*/field (:fields expr-obj))]))
  
(defmethod emit :new [expr-obj]
  (let [ctor (assoc-id (:ctor expr-obj))
        args (map assoc-id (:args expr-obj))]
    (concat [(assoc (emit-common expr-obj)
               :ast.new/ctor (:db/id ctor)
               :ast.new/args (map :db/id args))]
            (emit ctor)
            (mapcat emit args))))

(defmethod emit :default [expr-obj]
  (let [children (map assoc-id (:children expr-obj))]
    (concat [(assoc (emit-common expr-obj)
               :ast.default/op (:op expr-obj)
               :ast.default/child (map :db/id children))]
            (mapcat emit children))))

(defn emit-transaction-data [expr-obj]
  (let [expr-obj (assoc-id expr-obj)]
    (cons
     {:db/id (:db/id expr-obj)
      :ast/top-level true}
     (emit expr-obj))))
