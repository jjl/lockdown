(ns irresponsible.lockdown
  (:require [clojure.spec :as s]
            [irresponsible.lockdown.manager :refer [check chain]])
  (:import [java.lang.reflect Modifier]
           [java.security AccessController AccessControlContext PrivilegedAction]))

(s/def ::sec-mgr (partial instance? SecurityManager))
        
(defn sec-ex
  "Throws a SecurityException with the given message
   args: [msg] ; a string
   throws: SecurityException"
  [^String msg]
  (throw (SecurityException. msg)))

(defmacro with-context
  "Runs exprs within the provided AccessControlContext
   args: [ctx & exprs]
   returns: result of exprs"
  [AccessControlContext ctx & exprs]
  `(AccessController/doPrivileged ~(with-meta ctx {:tag AccessControlContext})
     (reify PrivilegedAction
        (run [_] ~@exprs))))

(defmacro lock-down
  "Runs exprs within the provided SecurityManager
   args: [security-manager & exprs]
   returns: result of exprs"
  [sm & exprs]
  `(let [secman# (System/getSecurityManager)
         ctx# (AccessController/getContext)]
     (try
       ~@exprs
       (finally
         (when (not= secman# (System/getSecurityManager))
          (with-context ctx#
            (System/setSecurityManager secman#)))))))

(s/fdef lock-down
  :args (s/cat :sec-mgr ::sec-mgr :exprs (s/* any?))
  :ret  ::security-manager)

(defmacro match-check
  "Creates a security-manager that checks against the provided
   core.match expressions. The contract is to throw a SecurityException
   if you are denying something, e.g. with `(sec-ex msg)`
   args: [& exprs] ; core.match body expressions
   returns: result of exprs"
  [& exprs]
  (when (not= 0 (mod (count exprs) 2))
    (throw (ex-info "match-check: requires an even number of expressions" {:got exprs})))
  (check exprs))

(defmacro match-chain
  "Like `match-check` but also consults the SecurityManager enabled at
   the time of creation
   args: [& exprs] ; core.match body expressions
   returns: result of exprs"
  [& exprs]
  (when (not= 0 (mod (count exprs) 2))
    (throw (ex-info "match-chain: requires an even number of expressions" {:got exprs})))
  `(let [chain# (System/getSecurityManager)]
     (chain chain# exprs)))
