(ns scape.core
  (:require [datomic.api :refer [db q] :as d]
            [scape.emitter :refer [emit-transaction-data]]
            [scape.analyze :refer [analyze-file deep-dissoc]]
            [scape.schema :refer [schema]]
            [scape.rules :as rules]
            [clojure.pprint :refer [pprint]]))

(comment
  (def files ["cljs/core.cljs"
              "cljs/reader.cljs"
              "clojure/set.cljs"
              "clojure/string.cljs"
              "clojure/walk.cljs"
              "clojure/zip.cljs"
              "clojure/core/reducers.cljs"
              "clojure/browser/dom.cljs"
              "clojure/browser/event.cljs"
              "clojure/browser/net.cljs"
              "clojure/browser/repl.cljs"
              "domina.cljs"
              "domina/css.cljs"
              "domina/events.cljs"
              "domina/support.cljs"
              "domina/xpath.cljs"])

  (def ast-db
    (let [uri "datomic:mem://ast"]
      (when (d/delete-database uri)
        (println uri "deleted."))
      (when (d/create-database uri)
        (println uri "created.")
      (let [conn (d/connect uri)]
        (d/transact conn schema)
        (println "Schema transaction complete.")
        (doseq [file files
                ast (analyze-file file)]
          (let [tdata (emit-transaction-data ast)]
            (d/transact conn tdata)))
        (println "AST transaction complete.")
        (db conn)))))

  ;; How many top-level forms?
  (count (q '[:find ?e
              :where
              [?e :ast/top-level true]]
            ast-db))
  
  ;; How many ast nodes are there in core.cljs?
  (count (q '[:find ?e
              :where
              [?e :ast/ns :cljs.core]
              [?e :ast/op]]
            ast-db))

  ;; What namespaces have been analyzed?
  (q '[:find ?ns
       :where
       [_ :ast/ns ?ns]]
     ast-db)

  ;; What ops are in use?
  (q '[:find ?op
       :where [_ :ast/op ?op]]
     ast-db)

  ;; Where are no-ops?
  (q '[:find ?line
       :where
       [?e :ast/op :no-op]
       [?e :ast/line ?line]]
     ast-db)
  
  ;; What forms are on line 288?
  (q '[:find ?form
       :where
       [?op :ast/op]
       [?op :ast/line 288]
       [?op :ast/form ?form]
       [?op :ast/ns :domina]]
     ast-db)

  ;; Find documentation and line number
  (q '[:find ?line ?doc
       :in $ ?name
       :where
       [?def :ast.def/name ?name]
       [?def :ast.def/doc ?doc]
       [?def :ast/line ?line]]
     ast-db :cljs.core/map-indexed)
  
  ;; On what lines (in domina) is the function 'map' used?
  (q '[:find ?line
       :in $ ?sym
       :where
       [?var :ast.var/ns-qualified-name ?sym]
       [?var :ast/line ?line]
       [?var :ast/ns :domina]]
     ast-db :cljs.core/map)
  
  ;; What are the most used var names?
  (->>  (q '[:find ?var ?sym
             :in $ ?local ?ns
             :where
             [?var :ast.var/name ?sym]
             [?var :ast/ns ?ns]]
           ast-db false :domina.events)
        (map second)
        frequencies
        (sort-by second)
        reverse)

  ;; Most used op's. 
  (->> (q '[:find ?e ?op
            :where
            [?e :ast/op ?op]]
          ast-db)
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
     ast-db)

  ;; How many invokations?
  (count (q '[:find ?invoke
              :where
              [?invoke :ast/op :invoke]]
            ast-db))

  ;; Enumerate the keywords used as function
  (q '[:find ?kw
       :where
       [?e :ast.invoke/f ?kw*]
       [?kw* :ast/op :constant]
       [?kw* :ast/form ?kw]]
     ast-db)

  ;; Any local functions invoked?
  (sort-by second
           (q '[:find ?name ?line
                :where
                [_ :ast.invoke/f ?local]
                [?local :ast/line ?line]
                [?local :ast.local/name ?name]]
              ast-db))

  ;;What op's are parents to recur?
  (->>
   (q '[:find ?op ?p
        :where
        [?e :ast/op :recur]
        [?p :ast/child ?e]
        [?p :ast/op ?op]]
      ast-db)
   (map first)
   frequencies)

  ;; What op's are defined?
  (->> 
   (q '[:find ?op ?init
        :in $ %
        :where
        [?e :ast/op :def]
        [?e :ast/child ?init]
        [?init :ast/op ?op]]
      ast-db)
   (map first)
   frequencies)

  ;; non-def top levels
  (sort-by second
           (q '[:find ?op ?line
                :where
                [?e :ast/op ?op]
                [?e :ast/top-level]
                [?e :ast/line ?line]
                [(not= ?op :def)]]
              ast-db))

  ;; What namespaces are used, and how many times?
  (->> (q '[:find ?ns ?var
            :where
            [?var :ast.var/name]
            [?var :ast.var/ns ?ns]]
          ast-db)
       (map first)
       frequencies
       (sort-by second)
       reverse)

  ;; What vars from namespace x are used in namespace y?
  (q '[:find ?var-name
       :in $ ?x ?y
       :where
       [?var :ast.var/ns ?x]
       [?var :ast/ns ?y]
       [?var :ast.var/name ?var-name]]
     ast-db :cljs.core :domina.events)
  
  ;; Who's calling my namespace?
  (q '[:find ?ns
       :in $ % ?my-ns
       :where
       [?var :ast/op :var]
       [?var :ast/ns ?ns]
       [namespace ?var ?my-ns]
       [(not= ?ns ?my-ns)]]
     ast-db rules/namespace :clojure.string)

  ;; Who's using my fn (and on what line)?
  (q '[:find ?ns ?line
       :in $ ?my-var
       :where
       [?var :ast.var/ns-qualified-name ?my-var]
       [?var :ast/line ?line]
       [?var :ast/ns ?ns]
       [?var :ast.var/ns ?my-ns]
       [(not= ?ns ?my-ns)]]
     ast-db :cljs.core/filter)

  ;; What vars (from other namespaces) are used from my ns?
  (q '[:find ?var-name
       :in $ ?my-ns
       :where
       [?var :ast/ns ?my-ns]
       [?var :ast.var/name ?var-name]
       [?var :ast.var/ns ?ns]
       [(not= ?ns ?my-ns)]]
     ast-db :domina.css)

  ;; Which function in core is used most (outside core itself)
  (->> (q '[:find ?var ?var-name
            :where
            [?var :ast.var/ns :cljs.core]
            [?var :ast.var/name ?var-name]
            [?var :ast/ns ?var-ns]
            [(not= ?var-ns :cljs.core)]]
          ast-db)
       (map second)
       frequencies
       (sort-by second)
       reverse)

  ;; What functions does ?my-fn invoke?
  (q '[:find ?fn-name
       :in $ % ?my-fn
       :where
       [?def :ast.def/name ?my-fn]
       [ancestor ?def ?invoke]
       [?invoke :ast.invoke/f ?f]
       [?f :ast.var/name ?fn-name]]
     ast-db
     (concat rules/ancestor rules/child)
     :cljs.core/map)

  (def transitive-var
    '[[[transitive-var ?e ?v]
       [ancestor ?e ?v]
       [?v :ast.var/name]]
      [[transitive-var ?e ?v]
       [ancestor ?e ?v*]
       [?v* :ast.var/ns-qualified-name ?name]
       [?ve :ast.def/name ?name]
       [transitive-var ?ve ?v]]])

  ;; Transitive var dependency
  ;; Could be used for tree shaking?
  (q '[:find ?var-name
       :in $ % ?my-def
       :where
       [?def :ast.def/name ?my-def]
       [transitive-var ?def ?var]
       [?var :ast.var/name ?var-name]]
     ast-db
     (concat transitive-var
             rules/ancestor
             rules/child)
     :cljs.core/map)

  )