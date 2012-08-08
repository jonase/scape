(ns scape.rules)

(def child
  '[[[child ?parent ?child]
     [?parent :ast.if/test ?child]]
    [[child ?parent ?child]
     [?parent :ast.if/then ?child]]
    [[child ?parent ?child]
     [?parent :ast.if/else ?child]]
    
    [[child ?parent ?child]
     [?parent :ast.def/init ?child]]
    
    [[child ?parent ?child]
     [?parent :ast.fn/method ?method]
     [?method :ast.fn.method/statement ?child]]
    [[child ?parent ?child]
     [?parent :ast.fn/method ?method]
     [?method :ast.fn.method/return ?child]]
    
    [[child ?parent ?child]
     [?parent :ast.do/statement ?child]]
    [[child ?parent ?child]
     [?parent :ast.do/return ?child]]
    
    [[child ?parent ?child]
     [?parent :ast.let/binding ?binding]
     [?binding :ast.let.binding/init ?child]]
    [[child ?parent ?child]
     [?parent :ast.let/statement ?child]]
    [[child ?parent ?child]
     [?parent :ast.let/return ?child]]
    
    [[child ?parent ?child]
     [?parent :ast.invoke/f ?child]]
    [[child ?parent ?child]
     [?parent :ast.invoke/arg ?child]]
    
    [[child ?parent ?child]
     [?parent :ast.recur/arg ?child]]
    
    [[child ?parent ?child]
     [?parent :ast.default/child ?child]]])

(def ancestor
  '[[[ancestor ?a ?d]
     [child ?a ?d]]
    [[ancestor ?a ?d]
     [child ?a ?x]
     [ancestor ?x ?d]]])

