(ns scape.core
  (:require [datomic.api :refer [db q] :as d]
            [scape.emitter :refer [emit-transaction-data]]
            [scape.analyze :refer [analyze-file deep-dissoc]]
            [scape.schema :refer [schema]]
            [clojure.pprint :refer [pprint]]))

(comment
  (def uri "datomic:mem://ast")
  
  (d/delete-database uri)
  (d/create-database uri)
  
  (def conn (d/connect uri))
  
  (d/transact conn schema)
  
  (doseq [ast (analyze-file "cljs/core.cljs")]
    (let [tdata (emit-transaction-data ast)]
      (d/transact conn tdata)))
  
  ;; how many transactions? i.e., top level forms
  (count (analyze-file "cljs/core.cljs"))
  ;; 502

  ;; Same as above
  (count (q '[:find ?e
              :where
              [?e :ast/top-level true]]
            (db conn)))
  
  ;; How many datoms is the above?
  (->> (analyze-file "cljs/core.cljs")
       (mapcat emit-transaction-data)
       count)
  ;; 147171 facts about cljs.core!
  
  ;; How many ast nodes are there in core.cljs?
  (count (q '[:find ?e
              :where
              [?e :ast/op]]
            (db conn)))
  
  ;; On what lines is the test part of an if statement a constant, and
  ;; what is that constant?
  (seq (q '[:find ?line ?form
            :where
            [?e :ast.if/test ?t]
            [?t :ast/op :ast.op/constant]
            [?t :ast/form ?form]
            [?t :ast/line ?line]]
          (db conn)))
  
  ;; What form is on line 291?
  (q '[:find ?form
       :where
       [?op :ast/op]
       [?op :ast/line 291]
       [?op :ast/form ?form]]
     (db conn))
  
  ;; Find documentation and line number
  (q '[:find ?line ?doc
       :in $ ?name
       :where
       [?def :ast/name ?name]
       [?def :ast.def/doc ?doc]
       [?def :ast/line ?line]]
     (db conn) "cljs.core.filter")
  
  ;; On what lines is the function 'map' used?
  (q '[:find ?line
       :in $ ?sym
       :where
       [?var :ast/op :ast.op/var]
       [?var :ast.var/local false]
       [?var :ast/form ?sym]
       [?var :ast/line ?line]]
     (db conn) "map")
  
  ;; What are the most used local/var names?
  (->>  (q '[:find ?var ?sym
             :in $ ?local
             :where
             [?var :ast.var/local ?local]
             [?var :ast/form ?sym]]
           (db conn) false)
        (map second)
        frequencies
        (sort-by second)
        reverse)
  
  ;; On what line is the return of a function method a constant and
  ;; what is the type of that constant?
  (q '[:find ?line ?type
       :where
       ;;[?fn :ast/op :ast.op/fn]
       [?_ :ast.fn/method ?fnm]
       [?fnm :ast/ret ?ret]
       [?ret :ast/op :ast.op/constant]
       [?ret :ast.constant/type ?type]
       [?ret :ast/line ?line]]
     (db conn))
  
  ;; Most used op's. Can this be combined into one query?
  (sort-by second
           (for [[op] (q '[:find ?op
                           :where
                           [?_ :ast/op ?op*]
                           [?op* :db/ident ?op]]
                         (db conn))]
             [op (count (q '[:find ?e
                             :in $ ?op
                             :where
                             [?e :ast/op ?op]]
                           (db conn) op))]))

  ;; On what lines is a loop used?
  (q '[:find ?line
       :where
       [?let :ast/op :ast.op/let]
       [?let :ast.let/loop true]
       [?let :ast/line ?line]]
     (db conn))

  ;; How many invokations?
  (count (q '[:find ?invoke
              :where
              [?invoke :ast/op :ast.op/invoke]]
            (db conn)))

  ;; Enumerate the keywords used as function
  (q '[:find ?kw
       :where
       [?e :ast.invoke/f ?kw*]
       [?kw* :ast/op :ast.op/constant]
       [?kw* :ast/form ?kw]]
     (db conn))

  ;; Any local functions used?
  (sort-by second
           (q '[:find ?name ?line
                :where
                [?e :ast.invoke/f ?var]
                [?var :ast.var/local true]
                [?var :ast/line ?line]
                [?var :ast/name ?name]]
              (db conn)))

  (def child-rules
    '[[(child ?parent ?child)
       [?parent :ast/op]
       [?parent :ast/statement ?child]]
      [(child ?parent ?child)
       [?parent :ast/op]
       [?parent :ast/ret ?child]]
      [(child ?parent ?child)
       [?parent :ast/child ?child]]
      [(child ?parent ?child)
       [?parent :ast/arg ?child]]
      [(child ?parent ?child)
       [?parent :ast.if/test ?child]]
      [(child ?parent ?child)
       [?parent :ast.if/then ?child]]
      [(child ?parent ?child)
       [?parent :ast.if/else ?child]]
      [(child ?parent ?child)
       [?parent :ast/op]
       [?parent :ast/init ?child]]
      [(child ?parent ?child)
       [?parent :ast.throw/expr ?child]]
      [(child ?parent ?child)
       [?parent :ast.fn/method ?method]
       [?method :ast/statement ?child]]
      [(child ?parent ?child)
       [?parent :ast.fn/method ?method]
       [?method :ast/ret ?child]]
      [(child ?parent ?child)
       [?parent :ast.let/binding ?binding]
       [?binding :ast/init ?child]]
      [(child ?parent ?child)
       [?parent :ast.invoke/f ?child]]])
      
  ;;What op's are parents to recur?
  (q '[:find ?op ?line
       :in $ %
       :where
       [?r :ast/op :ast.op/recur]
       [child ?p* ?r]
       [?p* :ast/op ?op*]
       [?op* :db/ident ?op]
       [?p* :ast/line ?line]]
     (db conn) child-rules)

  ;; What op's are defined?
  (->> 
   (q '[:find ?op ?init
        :in $ %
        :where
        [?def :ast/op :ast.op/def]
        [child ?def ?init]
        [?init :ast/op ?op*]
        [?op* :db/ident ?op]]
      (db conn) child-rules)
   (map first)
   frequencies)
  
  )