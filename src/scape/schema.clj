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

(def common
  [[:ast/line :long :one]
   [:ast/op :keyword :one]
   [:ast/ns :keyword :one]
   [:ast/form :string :one]])

(def top-level
  [[:ast/top-level :boolean :one]])

(def op-if
  [[:ast.if/test :ref :one]
   [:ast.if/then :ref :one]
   [:ast.if/else :ref :one]])

(def op-var
  [[:ast.var/name :keyword :one]
   [:ast.var/ns :keyword :one]
   [:ast.var/ns-qualified-name :keyword :one]])

(def op-local
  [[:ast.local/name :keyword :one]])

(def op-def
  [[:ast.def/name :keyword :one]
   [:ast.def/init :ref :one]
   [:ast.def/doc :string :one]])

(def op-fn
  [[:ast.fn/method :ref :many]
   [:ast.fn.method/statement :ref :many]
   [:ast.fn.method/return :ref :one]])

(def op-do
  [[:ast.do/statement :ref :many]
   [:ast.do/return :ref :one]])

(def op-constant
  [])

(def op-let
  [[:ast.let.binding/name :keyword :one]
   [:ast.let.binding/init :ref :one]
   [:ast.let/loop :boolean :one]
   [:ast.let/binding :ref :many]
   [:ast.let/statement :ref :many]
   [:ast.let/return :ref :one]])

(def op-invoke
  [[:ast.invoke/f :ref :one]
   [:ast.invoke/arg :ref :many]])

(def op-recur
  [[:ast.recur/arg :ref :many]])

(def op-deftype*
  [[:ast.deftype*/name :keyword :one]
   [:ast.deftype*/ns :keyword :one]
   [:ast.deftype*/ns-qualified-name :keyword :one]
   [:ast.deftype*/field :keyword :many]])

(def op-defrecord*
  [[:ast.defrecord*/name :keyword :one]
   [:ast.defrecord*/ns :keyword :one]
   [:ast.defrecord*/ns-qualified-name :keyword :one]
   [:ast.defrecord*/field :keyword :many]])

(def op-new
  [[:ast.new/ctor :ref :one]
   [:ast.new/args :ref :many]])

(def op-default
  [[:ast.default/op :keyword :one]
   [:ast.default/child :ref :many]])

(def schema
  (map #(apply attribute %)
       (concat common
               top-level
               op-if
               op-var
               op-local
               op-def
               op-fn
               op-do
               op-constant
               op-let
               op-invoke
               op-recur
               op-deftype*
               op-defrecord*
               op-new
               op-default)))


   
