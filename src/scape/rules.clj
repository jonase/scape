(ns scape.rules
  (:refer-clojure :exclude [namespace type]))

(def namespace
  '[[[namespace ?var ?ns]
     [?var :ast/name ?name]
     [(namespace ?name) ?ns-str]
     [(keyword ?ns-str) ?ns]]])

(def descendant 
  '[[[descendant ?r ?d]
     [?r :ast/child ?d]]
    [[descendant ?r ?d]
     [?r :ast/child ?x]
     [descendant ?x ?d]]])

(def top-level
  '[[[top-level ?tl ?e]
     [?t1 :ast/top-level true]
     [descendant ?t1 ?e]]])

(def form
  '[[[form ?e ?f]
     [?e :ast/form ?fs]
     [(read-string ?fs) ?f]]])

(def type
  '[[[type ?e ?t]
     [?e :ast.constant/type ?t]]
    [[type ?e ?t]
     ;[?e :ast/op]
     [?e :ast/ret ?r]
     [type ?r ?t]]
    [[type ?e ?t]
     [?e :ast.fn/method ?m]
     [?m :ast/ret ?r]
     [type ?r ?t]]])

(def transitive-var-dep
  '[[[transitive-var-dep ?e ?v]
     [descendant ?e ?v]
     [?v :ast/op :var]
     [?v :ast.var/local false]]
    [[transititve-var-dep ?e ?v]
     [descendant ?e ?v*]
     [?v* :ast/op :var]
     [?v* :ast.var/local false]
     [?ve :db/ident ?v*]
     [transitive-var-dep ?ve ?v]]])

    