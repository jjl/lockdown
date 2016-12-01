(ns irresponsible.lockdown
  (:require [clojure.spec :as s])
  (:import [java.lang.reflect Modifier]
           [java.security
            AccessController AccessControlContext
            Permission PrivilegedAction]))

(s/def ::sec-mgr (partial instance? SecurityManager))

(defn sec-ex
  "Throws a SecurityException with the given message
   args: [msg] ; a string
   throws: SecurityException"
  [msg]
  (throw (SecurityException. ^String (str msg))))

(defmacro with-current-sec-man
  ""
  [name & exprs]
  `(let [~(with-meta name {:tag SecurityManager})
         (System/getSecurityManager)]
     ~@exprs))

(defn can?
  "Checks whether you can perform a given action
   args: [perm] ; perm is java.lang.Permission
   throws: SecurityException if you can't"
  [^Permission perm]
  (with-current-sec-man secman
    (or (nil? secman)
        (.checkPermission secman perm))))

(defmacro with-context
  "Runs exprs within the provided AccessControlContext
   args: [ctx & exprs]
   returns: result of exprs"
  [ctx & exprs]
  `(AccessController/doPrivileged
     (reify PrivilegedAction
       (~'run [^PrivilegedAction this#] ~@exprs))
     ~(with-meta ctx {:tag AccessControlContext})))

;; (s/def ::with-context
;;   (s/cat :adp `#{AccessController/doPrivileged}
;;          :acc (comp :tag meta)
;;          :reify (s/spec
;;                  (s/cat :reify `#{reify}
;;                         :pa `#{PrivilegedAction}
;;                         :run (s/spec
;;                               (s/cat :run '#{run}
;;                                      :v vector?
;;                                      :exprs (s/* any?)))))))

(defmacro sec-man
  "Returns an anonymous security manager with checks done by
   `exprs` with the java.security.Permission bound to perm-name
   args: [perm-name & exprs]
   returns: code for anonymous security manager"
  [p-name & exprs]
  `(proxy [SecurityManager] []
     (checkPermission [~(with-meta p-name {:tag Permission})]
       ~@exprs)))

(defmacro chained-sec-man
  "Like sec-man, but also calls the current security-manager
   note: 'current' is defined as 'at time of evaluation'
   args: [perm-name & exprs]
   returns: code for anonymous security manager"
  [p-name & exprs]
  (let [old-name (with-meta `old# {:tag SecurityManager})]
    `(let [~old-name (System/getSecurityManager)]
       (proxy [SecurityManager] []
         (checkPermission [~p-name]
           ~@exprs
           (.checkPermission ~old-name ~p-name))))))

(defmacro lock-down
  "Runs exprs within the provided SecurityManager
   args: [security-manager & exprs]
   returns: result of exprs"
  [sm & exprs]
  `(let [^SecurityManager secman# (System/getSecurityManager)
         ^AccessControlContext ctx# (AccessController/getContext)]
     (try
       (System/setSecurityManager ~(with-meta sm {:tag SecurityManager}))
       ~@exprs
       (finally
         (when (not (identical? secman# (System/getSecurityManager)))
           (with-context ctx#
             (System/setSecurityManager secman#)))))))

;; (lock-down (System/getSecurityManager) (sec-ex "false"))


;;(s/fdef lock-down
;;  :args (s/spec (s/+ (s/* any?)))
;;  :ret  ::sec-mgr)






