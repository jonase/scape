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

(defn ns-calls
  []
  (q '[:find ?source ?target
       :where
       [?var :ast/ns ?source]
       [?var :ast.var/ns ?target]
       [(not= ?source ?target)]]
     (-> uri d/connect db)))

(defn digraph [name data]
  (with-open [w (io/writer (str name ".dot"))]
    (binding [*out* w]
      (println "digraph g {")
      (doseq [[source target] data]
        (printf "\"%s\" -> \"%s\";\n" source target))
      (println "}"))))

  
(comment
  ;; Calls from domina to another ns
  (digraph "callgraph" (domina-calls))

  ;; Calls from one ns to another
  (digraph "nsgraph" (ns-calls))
  )
