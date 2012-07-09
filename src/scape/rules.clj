(ns scape.rules
  (:refer-clojure :exclude [namespace]))

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