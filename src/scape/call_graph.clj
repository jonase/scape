(ns scape.call-graph
  (:require [datomic.api :refer [q db] :as d]
            [scape.rules :as rules]
            [clojure.java.io :as io]))

(def uri "datomic:mem://ast")

(defn domina-calls []
  (q '[:find ?source ?target
       :in $ %
       :where
       [?e :ast.def/name ?source]
       [?e :ast/ns :domina]
       [ancestor ?e ?d]
       [?d :ast.var/ns :domina]
       [?d :ast.var/ns-qualified-name ?target]]
     (-> uri d/connect db)
     (concat rules/ancestor rules/child)))

(comment

  (with-open [w (io/writer "callgraph.dot")]
    (binding [*out* w]
      (println "digraph g {")
      (doseq [[source target] (domina-calls)]
        (printf "\"%s\" -> \"%s\";\n" source target))
      (println "}")))

  )
