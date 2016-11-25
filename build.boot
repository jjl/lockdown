(set-env!
  :project 'irresponsible/lockdown
  :version "0.1.0"
  :resource-paths #{"src"}
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                  [org.clojure/core.match "0.3.0-alpha4"]
                  [camel-snake-kebab "0.4.0"]
                  [adzerk/boot-test "1.1.0" :scope "test"]])

(require '[adzerk.boot-test :as t])

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)
       :description "Java security, clojure simplicity"
       :url "https://github.com/irresponsible/lockdown"
       :scm {:url "https://github.com/irresponsible/lockdown.git"}
       :license {"MIT" "https://en.wikipedia.org/MIT_License"}}
  target  {:dir #{"target"}})

(deftask testing []
  (set-env! :source-paths #(conj % "test")
            :resource-paths #(conj % "test"))
  identity)

(deftask test []
  (testing)
  (comp (speak) (t/test)))

(deftask autotest []
  (comp (watch) (test)))

(deftask make-jar []
  (comp (pom) (jar)))

(deftask travis []
  (testing)
  (t/test))

