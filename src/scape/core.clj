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

  (def child-rule
    '[
      #_[[child ?parent ?child]
         [?parent :ast.if/test ?child]]
      #_[[child ?parent ?child]
         [?parent :ast.if/then ?child]]
      #_[[child ?parent ?child]
         [?parent :ast.if/else ?child]]
      
      #_[[child ?parent ?child]
         [?parent :ast.def/init ?child]]

      #_[[child ?parent ?child]
         [?parent :ast.fn/method ?method]
         [?method :ast.fn.method/statement ?child]]
      #_[[child ?parent ?child]
         [?parent :ast.fn/method ?method]
         [?method :ast.fn.method/return ?child]]
      
      #_[[child ?parent ?child]
         [?parent :ast.do/statement ?child]]
      #_[[child ?parent ?child]
         [?parent :ast.do/return ?child]]
      
      #_[[child ?parent ?child]
         [?parent :ast.let/binding ?binding]
         [?binding :ast.let.binding/init ?child]]
      #_[[child ?parent ?child]
         [?parent :ast.let/statement ?child]]
      #_[[child ?parent ?child]
         [?parent :ast.let/return ?child]]
      
      #_[[child ?parent ?child]
         [?parent :ast.invoke/f ?child]
         [?parent :ast.invoke/arg ?child]]
      
      #_[[child ?parent ?child]
         [?parent :ast.recur/arg ?child]]
      
      [[child ?a ?d]
       [?a :ast.default/child ?d]]])
  
  (def ancestor
    '[[[ancestor ?ancestor ?descendant]
       [child ?ancestor ?descendant]]
      [[ancestor ?ancestor ?descendant]
       [?ancestor :ast.default/child ?middle-man]
       ;[child ?ancestor ?middle-man]
       [ancestor ?middle-man ?descendant]]])

  (q '[:find ?descendant
       :in $ %
       :where
       [?map :ast.def/name :cljs.core/map]
       [?map :ast.def/init ?init]
       [ancestor ?init ?descendant]]
     ast-db
     (concat ancestor child-rule))
  
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
       [?var :ast/op :var]
       [?var :ast.var/local false]
       [?var :ast/name ?sym]
       [?var :ast/line ?line]
       [?var :ast/ns :domina]]
     ast-db :cljs.core/map)
  
  ;; What are the most used local/var names?
  (->>  (q '[:find ?var ?name
             :where
             [?var :ast.var/name ?name]]
           ast-db)
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

  ;; Any local functions used?
  (sort-by second
           (q '[:find ?name ?line
                :where
                [?e :ast.invoke/f ?var]
                [?var :ast.var/local true]
                [?var :ast/line ?line]
                [?var :ast/name ?name]]
              ast-db))

  ;;What op's are parents to recur?
  (->>
   (q '[:find ?op ?p
        :in $ %
        :where
        [?e :ast/op :recur]
        [child ?p ?e]
        [?p :ast/op ?op]]
      ast-db rules/child)
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
      ast-db rules/child)
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
            [?var :ast/op :var]
            [?var :ast/name ?name]
            [?var :ast.var/local false]
            [(namespace ?name) ?ns]]
          ast-db)
       (map first)
       frequencies
       (sort-by second)
       reverse)

  ;; What vars from namespace x are used in namespace y?
  (q '[:find ?var-name
       :in $ % ?x ?y
       :where
       [?var :ast/op :var]
       [?var :ast/name ?var-name]
       [?var :ast/ns ?y]
       [namespace ?var ?x]]
     ast-db rules/namespace :cljs.core :domina.events)
  
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
       :in $ % ?my-var
       :where
       [?var :ast/name ?my-var]
       [?var :ast/line ?line]
       [?var :ast/ns ?ns]
       [namespace ?var ?my-ns]
       [(not= ?ns ?my-ns)]]
     ast-db rules/namespace :cljs.core/filter)

  ;; What vars (from other namespaces) are used from my ns?
  (q '[:find ?var-name
       :in $ % ?my-ns
       :where
       [?var :ast/name ?var-name]
       [?var :ast/ns ?my-ns]
       [namespace ?var ?ns]
       [(not= ?ns ?my-ns)]
       [(not= ?ns nil)]]
     ast-db rules/namespace :domina.css)

  ;; Which function in core is used most (outside core itself)
  (->> (q '[:find ?var ?var-name
            :in $ %
            :where
            [?var :ast/op :var]
            [?var :ast/name ?var-name]
            [?var :ast/ns ?var-ns]
            [namespace ?var :cljs.core]
            [(not= ?var-ns :cljs.core)]]
          ast-db
          rules/namespace)
       (map second)
       frequencies
       (sort-by second)
       reverse)

  ;; What op's can we figure out the type of?
  (q '[:find ?op
       :in $ %
       :where
       [?e :ast/op ?op]
       [type ?e _]]
     ast-db rules/type)

  ;; Enumerate type/op/line/ns of known types
  (seq (q '[:find ?type ?op ?line ?ns
            :in $ %
            :where
            [?e :ast/op ?op]
            [?e :ast/line ?line]
            [?e :ast/ns ?ns]
            [type ?e ?type]]
          ast-db rules/type))
       
  ;; What functions does ?my-fn invoke?
  (q '[:find ?fn-name
       :in $ % ?my-fn
       :where
       [?def :db/ident ?my-fn]
       [descendant ?def ?invoke]
       [?invoke :ast/op :invoke]
       [?invoke :ast.invoke/f ?f]
       [?f :ast.var/local false]
       [?f :ast/name ?fn-name]]
     ast-db rules/descendant :cljs.core/map)

  ;; Transitive var dependency
  ;; Could be used for tree shaking?
  (q '[:find ?var-name
       :in $ % ?my-fn
       :where
       [?def :db/ident ?my-fn]
       [transitive-var-dep ?def ?var]
       [?var :ast/name ?var-name]
       ]
     ast-db
     (concat rules/descendant
             rules/transitive-var-dep)
     :cljs.core/map)

  ;;=>
  ;; #<HashSet [[:cljs.core/chunk-append],
  ;;            [:cljs.core/seq],
  ;;            [:cljs.core/rest],
  ;;            [:cljs.core/chunk],
  ;;            [:cljs.core/every?],
  ;;            [:cljs.core/conj],
  ;;            [:cljs.core/chunk-buffer],
  ;;            [:cljs.core/first],
  ;;            [:cljs.core/cons],
  ;;            [:cljs.core/chunk-cons],
  ;;            [:cljs.core/apply],
  ;;            [:cljs.core/count],
  ;;            [:cljs.core/-nth],
  ;;            [:cljs.core/chunk-rest],
  ;;            [:cljs.core/chunked-seq?],
  ;;            [:cljs.core/chunk-first],
  ;;            [:cljs.core/identity],
  ;;            [:cljs.core/LazySeq]]>
       
  
  )