(ns scape.analyze
  (:require [cljs.compiler]
            [cljs.analyzer :as a]
            [clojure.java.io :as io]
            [clojure.walk :as walk]))


(defn ast-seq [ast]
  (tree-seq :children :children ast))

(defn analyze-file [file]
  (let [res (if (= \/ (first file)) file (io/resource file))]
    (assert res (str "Can't find " file " in classpath"))
    (binding [a/*cljs-ns* 'cljs.user
              a/*cljs-file* (.getPath ^java.net.URL res)]
      (with-open [r (io/reader res)]
        (let [env {:ns (@a/namespaces a/*cljs-ns*)
                   :context :statement
                   :locals {}}
              pbr (clojure.lang.LineNumberingPushbackReader. r)
              eof (Object.)]
          (loop [r (read pbr false eof false) asts (transient [])]
            (let [env (assoc env :ns (@a/namespaces a/*cljs-ns*))]
              (if-not (identical? eof r)
                (recur (read pbr false eof false)
                       (conj! asts (assoc (a/analyze env r) :original-form r)))
                (persistent! asts)))))))))

(defn op= [ast op]
  (= (:op ast) op))


(def default-env {:ns 'cljs.user
                  :context :statement
                  :locals {}})

(defn deep-dissoc [coll & ks]
  (walk/prewalk #(if (map? %) (apply dissoc % ks) %)
                coll))
