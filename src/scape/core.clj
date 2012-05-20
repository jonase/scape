(ns scape.core
  (:require [datomic.api :refer [db q] :as d]
            [scape.emitter :refer [emit-transaction-data]]
            [scape.analyze :refer [analyze-file ast-seq op= default-env]]
            [scape.schema :refer [schema]]))
            

(comment 
  (def uri "datomic:mem://ast")

  (d/delete-database uri)
  (d/create-database uri)
  
  (def conn (d/connect uri))

  (d/transact conn schema)
  
  (doseq [ast (analyze-file "cljs/core.cljs")]
    (println "Transacting " (:op ast))
    (d/transact conn (emit-transaction-data ast))
    (println "Done."))

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

  )
