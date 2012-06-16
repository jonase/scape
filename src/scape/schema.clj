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
    :db/ident :ast/top-level
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Top level form?"
    :db.install/_attribute :db.part/db}
   
   {:db/id #db/id[:db.part/db]
    :db/ident :ast/statement
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "A block statement"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :ast/ret
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "A block return expression"
    :db.install/_attribute :db.part/db}
   
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
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "name"
    :db.install/_attribute :db.part/db}
   
   {:db/id #db/id[:db.part/db]
    :db/ident :ast/ns
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "namespace"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :ast/arg
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Arguments"
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
    :db/ident :ast/init
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "init"
    :db.install/_attribute :db.part/db}

   ;; :var
   {:db/id #db/id[:db.part/db]
    :db/ident :ast.var/local
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "is the var local or not?"
    :db.install/_attribute :db.part/db}

   ;; :fn
   {:db/id #db/id[:db.part/db]
    :db/ident :ast.fn/method
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "A function method"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :ast.fn/variadic
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/many
    :db/doc "A function method"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :ast.fn/fixed-arity
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/many
    :db/doc "A function method"
    :db.install/_attribute :db.part/db}

   ;; Throw
   {:db/id #db/id[:db.part/db]
    :db/ident :ast.throw/expr
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Throw expression"
    :db.install/_attribute :db.part/db}

   ;; Let
   {:db/id #db/id[:db.part/db]
    :db/ident :ast.let/loop
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Is this let expr a loop?"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :ast.let/binding
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Let or loop binding"
    :db.install/_attribute :db.part/db}

   ;; :invoke
   {:db/id #db/id[:db.part/db]
    :db/ident :ast.invoke/f
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Function being invoked"
    :db.install/_attribute :db.part/db}

   ])
