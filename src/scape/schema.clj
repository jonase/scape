(ns scape.schema)

(def schema
  [;; Common
   {:db/id #db/id[:db.part/db -1]
    :db/ident :ast/op
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc ":op"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/if}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/throw}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/try*}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/def}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/fn}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/do}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/let}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/recur}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/quote}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/new}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/set!}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/ns}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/deftype*}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/defrecord*}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/dot}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/js}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/invoke}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/var}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/map}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/vector}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/set}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/meta}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/constant}

   {:db/id #db/id[:db.part/user]
    :db/ident :ast.op/no-op}

   {:db/id #db/id[:db.part/db]
    :db/ident :ast/child
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "A child of an ast node"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :ast/form
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "String representation of the form"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :ast/line
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Line number, or -1 if not available"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :ast/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "name"
    :db.install/_attribute :db.part/db}
   
   {:db/id #db/id[:db.part/db]
    :db/ident :ast/ns
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "namespace"
    :db.install/_attribute :db.part/db}
      
   ;; :constant
   {:db/id #db/id[:db.part/db]
    :db/ident :ast.constant/type
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Type string of constant"
    :db.install/_attribute :db.part/db}

   ;; :if
   {:db/id #db/id[:db.part/db]
    :db/ident :ast.if/test
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Test part of an if expression"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :ast.if/then
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Then part of an if expression"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :ast.if/else
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Else part of an if expression"
    :db.install/_attribute :db.part/db}

   ;; :def
   {:db/id #db/id[:db.part/db]
    :db/ident :ast.def/init
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "init"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :ast.def/doc
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "doc"
    :db.install/_attribute :db.part/db}

   ;; :var
   {:db/id #db/id[:db.part/db]
    :db/ident :ast.var/local
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "is the var local or not?"
    :db.install/_attribute :db.part/db}
   
   ])

;; I should use :children. How do I refer to its :parent? can this be
;; done in datomic? Should :children be a multivalued :ref?
;; Should probably be named :child. Also, we'd need a :toplevel