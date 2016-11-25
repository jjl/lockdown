(ns irresponsible.lockdown.manager
  (:require [clojure.spec :as s]
            [clojure.string :as str]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clojure.core.match :refer [match]])
  (:import [java.lang.reflect Method Modifier]))

(defn kebab-name
  "Turns a method name of the form check* into a friendly clojure
   keyword by removing the 'check' and kebab-casing it
   args: [name] ; string
   returns: keyword"
  [^String name]
  (-> name
      (.substring 5) ; remove "check" from the front
      ->kebab-case-keyword))

(defn read-method
  "Given a relected method, turn it into a map of the data we need
   args: [m]
   returns: map with keys :name, :params"
  [^Method m]
  (let [^String name (.getName m)]
    (when (and (Modifier/isPublic (.getModifiers m))
               (.startsWith name "check"))
      (let [params (map (memfn getName) (.getParameterTypes m))]
        {:name name :params params}))))

(def method-data
  "Vector of clojure data for all check* methods
   Generated against the live SecurityManager"
  (into [] (keep read-method) (.getDeclaredMethods SecurityManager)))

(def signal-names
  "Vector of all signal names, generated against the live SecurityManager"
  (into #{} (map (comp kebab-name :name)) method-data))

(defn make-names
  "Given a list of parameter types, generates names for them
   and assoc :tag metadata to them, returning as a vector:
   args: [params] ;list of tags
   returns: vector of symbols with metadata"
  [params]
  (mapv #(with-meta % {:tag %2})
        (repeatedly (count params) gensym)
        params))

(defn check-method
  "Generates a method for 'match-check'
   args: [method-data handler-name]
   returns: code for a proxy method"
  [{:keys [name params]} handler]
  (let [names   (repeatedly (count params) gensym)
        params2 (mapv #(with-meta % {:tag %2}) names params)]
    `(~(symbol name) ~params2
      (~handler ~(kebab-name name) ~@names))))

(defn chain-method
  "Generates a method for 'match-chain'
   args: [next-sec-man method-data handler-name]
   returns: code for a proxy method"
  [next-sm {:keys [name params]} handler]
  (let [names   (repeatedly (count params) gensym)
        params2 (mapv #(with-meta % {:tag %2}) names params)]
    `(~(symbol name) ~params2
      (~handler ~(kebab-name name) ~@names)
      (. ~next-sm ~(symbol name) ~@names))))

(defn generate
  "Generates a proxy for SecurityManager
   args: [method-gen exprs]
     method-gen: function to generate methods
     exprs: expressions to embed in the handler function
   returns: code for proxy"
  [method-gen exprs]
  (let [handler `handler#]
    `(let [~handler (fn [& args#] (match args# ~@exprs))]
       (proxy [SecurityManager] []
         ~@(map #(method-gen % handler) method-data)))))

(defn check
  "Generates the code for the `match-check` macro
   args: [exprs]
   returns: code"
  [exprs]
  (generate check-method exprs))

(defn chain
  "Generates the code for the `match-check` macro
   args: [next-sec-man exprs]
   returns: code"
  [next-sm exprs]
  (generate (partial chain-method next-sm) exprs))

