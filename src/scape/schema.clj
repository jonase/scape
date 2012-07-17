(ns scape.schema
  (:use [datomic.api :only [tempid]]))

(defn attribute
  ([ident type cardinality]
     (attribute ident type cardinality nil))
  ([ident type cardinality doc]
     (merge
      {:db/id (tempid :db.part/db)
       :db/ident ident
       :db/valueType (keyword "db.type" (name type))
       :db/cardinality (keyword "db.cardinality" (name cardinality))
       :db.install/_attribute :db.part/db}
      (when doc
        {:db/doc doc}))))

(def schema
  (map #(apply attribute %)
       [[:ast/op             :keyword :one]
        [:ast/top-level      :boolean :one]
        [:ast/statement      :ref     :many]
        [:ast/ret            :ref     :one]
        [:ast/child          :ref     :many]
        [:ast/form           :string  :one]
        [:ast/line           :long    :one]
        [:ast/name           :keyword :one]
        [:ast/ns             :keyword :one]
        [:ast/arg            :ref     :many]
        [:ast.constant/type  :string  :one]
        [:ast.if/test        :ref     :one]
        [:ast.if/then        :ref     :one]
        [:ast.if/else        :ref     :one]
        [:ast/init           :ref     :one]
        [:ast.var/local      :boolean :one]
        [:ast.fn/method      :ref     :many]
        [:ast.fn/variadic    :boolean :one]
        [:ast.fn/fixed-arity :long    :one]
        [:ast.throw/expr     :ref     :one]
        [:ast.let/loop       :boolean :one]
        [:ast.let/binding    :ref     :many]
        [:ast.invoke/f       :ref     :one]]))
   

   
