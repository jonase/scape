(ns scape.rules
  (:refer-clojure :exclude [namespace type]))

(def child
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

(def namespace
  '[[[namespace ?var ?ns]
     [?var :ast/name ?name]
     [(namespace ?name) ?ns-str]
     [(keyword ?ns-str) ?ns]]])

(def descendant 
  '[[[descendant ?r ?d]
     [child ?r ?d]]
    [[descendant ?r ?d]
     [child ?r ?x]
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
