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
  ;; 538

  ;; Same as above
  (count (q '[:find ?e
              :where
              [?e :ast/top-level true]]
            (db conn)))
  
  ;; How many datoms is the above?
  (->> (analyze-file "cljs/core.cljs")
       (mapcat emit-transaction-data)
       count)
  ;; 183011 facts about cljs.core!
  
  ;; How many ast nodes are there in core.cljs?
  (count (q '[:find ?e
              :where
              [?e :ast/op]]
            (db conn)))

  ;; What namespaces have been analyzed?
  (q '[:find ?ns
       :where
       [_ :ast/ns ?ns]]
     (db conn))

  (q '[:find ?file
       :where
       [_ :ast/file ?file]]
     (db conn))

  ;; What ops are in use?
  (q '[:find ?op
       :where [_ :ast/op ?op]]
     (db conn))

  ;; Where are no-ops?
  (q '[:find ?line
       :where
       [?e :ast/op :no-op]
       [?e :ast/line ?line]]
     (db conn))
  
  ;; On what lines is the test part of an if statement a constant, and
  ;; what is that constant?
  (seq (q '[:find ?line ?form
            :where
            [?e :ast.if/test ?t]
            [?t :ast/op :constant]
            [?t :ast/form ?form]
            [?t :ast/line ?line]]
          (db conn)))
  
  ;; What form is on line 288?
  (q '[:find ?form
       :where
       [?op :ast/op]
       [?op :ast/line 288]
       [?op :ast/form ?form]]
     (db conn))
  
  ;; Find documentation and line number
  (q '[:find ?line ?doc
       :in $ ?name
       :where
       [?def :db/ident ?name]
       [?def :db/doc ?doc]
       [?def :ast/line ?line]]
     (db conn) :cljs.core/map-indexed)
  
  ;; On what lines is the function 'map' used?
  (q '[:find ?line
       :in $ ?sym
       :where
       [?var :ast/op :var]
       [?var :ast.var/local false]
       [?var :ast/name ?sym]
       [?var :ast/line ?line]]
     (db conn) :cljs.core/map)
  
  ;; What are the most used local/var names?
  (->>  (q '[:find ?var ?sym
             :in $ ?local
             :where
             [?var :ast.var/local ?local]
             [?var :ast/name ?sym]]
           (db conn) false)
        (map second)
        frequencies
        (sort-by second)
        reverse)

  ;; On what line is the return of a function method a constant and
  ;; what is the type of that constant?
  (q '[:find ?line ?type
       :where
       [?_ :ast.fn/method ?fnm]
       [?fnm :ast/ret ?ret]
       [?ret :ast/op :constant]
       [?ret :ast.constant/type ?type]
       [?ret :ast/line ?line]]
     (db conn))
  
  ;; Most used op's. 
  (->> (q '[:find ?e ?op
            :where
            [?e :ast/op ?op]]
          (db conn))
       (map second)
       frequencies
       (sort-by second)
       reverse)
            

  ;; On what lines is a loop used?
  (q '[:find ?line
       :where
       [?let :ast/op :let]
       [?let :ast.let/loop true]
       [?let :ast/line ?line]]
     (db conn))

  ;; How many invokations?
  (count (q '[:find ?invoke
              :where
              [?invoke :ast/op :invoke]]
            (db conn)))

  ;; Enumerate the keywords used as function
  (q '[:find ?kw
       :where
       [?e :ast.invoke/f ?kw*]
       [?kw* :ast/op :constant]
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
  (->>
   (q '[:find ?op ?p
        :in $ %
        :where
        [?e :ast/op :recur]
        [child ?p ?e]
        [?p :ast/op ?op]]
      (db conn) child-rules)
   (map first)
   frequencies)

  ;; What op's are defined?
  (->> 
   (q '[:find ?op ?init
        :in $ %
        :where
        [?e :ast/op :def]
        [child ?e ?init]
        [?init :ast/op ?op]]
      (db conn) child-rules)
   (map first)
   frequencies)

  ;; non-def top levels
  (sort-by second
           (q '[:find ?op ?line
                :where
                [?e :ast/op ?op]
                [?e :ast/top-level]
                [?e :ast/line ?line]
                [(not= ?op :ast.op/def)]]
              (db conn)))

  ;; What namespaces are used, and how many times?
  (->> (q '[:find ?ns ?var
            :where
            [?var :ast/op :var]
            [?var :ast/name ?name]
            [?var :ast.var/local false]
            [(namespace ?name) ?ns]]
          (db conn))
       (map first)
       frequencies
       (sort-by second)
       reverse)
  ;; =>
  '(["cljs.core" 3402]
    ["cljs" 167]
    ["goog" 99]
    ["js" 70]
    ["cljs.core.BitmapIndexedNode" 11]
    ["cljs.core.PersistentVector" 11]
    ["cljs.core.List" 8]
    ["cljs.core.PersistentArrayMap" 7]
    ["cljs.core.PersistentHashMap" 7]
    ["goog.string" 6]
    ["cljs.core.ObjMap" 6]
    ["Math" 6]
    ["goog.object" 5]
    ["cljs.core.HashMap" 4]
    ["cljs.core.Vector" 4]
    ["cljs.core.PersistentTreeSet" 3]
    ["cljs.core.PersistentTreeMap" 3]
    ["goog.array" 3]
    ["cljs.core.PersistentHashSet" 3]
    ["cljs.core.PersistentQueue" 2])
  
  )