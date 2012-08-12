# Scape

Scape uses the
[ClojureScript](https://github.com/clojure/clojurescript) analyzer to
emit [Datomic](http://datomic.com) transaction data containing useful
information about some ClojureScript codebase and putting the data in
a datomic database so the program becomes queriable via datalog. The
idea is to extract interesting statistics and facts about
ClojureScript programs.

This is work in progress. Take a look at
[core.clj](https://github.com/jonase/scape/blob/master/src/scape/core.clj)
and
[emitter.clj](https://github.com/jonase/scape/blob/master/src/scape/emitter.clj)
if you're interested to see how it works.

## Example

The following query

    [:find ?source ?target
     :in $ %
     :where
     [?e :ast.def/name ?source]
     [?e :ast/ns :domina]
     [ancestor ?e ?d]
     [?d :ast.var/ns :domina]
     [?d :ast.var/ns-qualified-name ?target]]

finds all definitions in the `domina` namespace together with all
(internal) calls those defs make. The code in the
[`call-graph`](https://github.com/jonase/scape/blob/master/src/scape/call_graph.clj)
namespace generates a graphviz file which can be seen rendered
[here](http://i.imgur.com/um0XO.png).

## License

Copyright © 2012 Jonas Enlund

Distributed under the Eclipse Public License, the same as Clojure.
