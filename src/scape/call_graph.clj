(ns scape.call-graph
  (:require [datomic.api :refer [q db] :as d]
            [scape.rules :as rules]
            [clojure.java.io :as io]))

(def uri "datomic:mem://ast")

(defn domina-calls []
  (q '[:find ?source ?target
       :in $ %
       :where
       [?e :db/ident ?source]
       [?e :ast/ns :domina]
       [?d :ast/op :var]
       [?d :ast.var/local false]
       [namespace ?d :domina]
       [?d :ast/name ?target]
       [descendant ?e ?d]]
     (-> uri d/connect db)
     (concat rules/descendant rules/namespace)))

(comment

  (with-open [w (io/writer "callgraph.dot")]
    (binding [*out* w]
      (println "digraph g {")
      (doseq [[source target] (domina-calls)]
        (printf "\"%s\" -> \"%s\";\n" source target))
      (println "}")))

  )
