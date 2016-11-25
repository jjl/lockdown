The irresponsible clojure guild presents...

# Lockdown

Java security, clojure simplicity

## Rationale

The JVM comes with comprehensive security features, but using them
correctly can be tedious. We've decomplected the API a bit to make it
easier to use

Note: Currently requires clojure 1.9 alpha!

## Usage

```clojure
(ns my.ns
 (:require [irresponsible.lockdown :as l]))
;; lock-down enables a security manager for the scope
;; match-chain creates a security manager with core.match
(l/lock-down (l/match-chain
               [:exit _] (throw (SecurityException. "Denied"))
			   :else nil) ;; default permit
  (run-user-code)) ; won't be able to exit
```

## API

`(sec-ex ^String msg)`

Throws a SecurityException with the given message

e.g. `(sec-ex "Access Denied!")`

`(with-context ^AccessControlContext ctx & exprs)`

ctx is an AccessControlContext as returned from `(AccessController/getContext)`
exprs will be evaluated within the given context

e.g. `(with-context (AccessController/getContext) (do-stuff))`

### `(lock-down security-manager & exprs)`

security-manager is a SecurityManager as returned from `(System/getSecurityManager)`
exprs will be evaluated with the given SecurityManager in effect

e.g. `(lock-down (System/getSecurityManager) (do-stuff))`

### `(match-check & exprs)`

Creates and returns a SecurityManager that converts the check into a
clojure vector and uses core.match expressions to check it.

e.g. `(match-check [:delete file] (sec-ex "No file deletions!") :else nil)`

### `(match-chain & exprs)`

Like match-check, but checks with the old SecurityManager if the
matches do not throw.

## Check signal list

* `[:accept ^String host ^int port]`
* `[:access thread-or-threadgroup]`
* `[:connect ^String host ^int port]`
* `[:connect ^String host ^int port context]]`
* `[:create-class-loader]`
* `[:delete ^String file]`
* `[:exec ^String cmd]`
* `[:exit ^int status]`
* `[:link ^String lib]`
* `[:listen ^int port]`
* `[:multicast ^InetAddress maddr]`
* `[:package-access ^String pkg]`
* `[:package-definition ^String pkg]`
* `[:permission ^Permission perm]`
* `[:print-job-access]`
* `[:property-access ^String key]`
* `[:properties-access]`
* `[:read ^FileDescriptor fd]`
* `[:set-factory]`
* `[:security-access ^String target]`
* `[:write fd-or-filename]`

### Marked deprecated

These events are proxied anyway, but code shouldn't be relying on them now
as they are marked as deprecated in JDK8.

`[:awt-event-queue-access]`
`[:member-access ^Class class ^int which]`
`[:system-clipboard-access]`
`[:top-level-window]`

## Copyright and License

MIT LICENSE

Copyright (c) 2016 James Laver

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
