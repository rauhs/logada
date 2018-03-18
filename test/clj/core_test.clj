(ns core-test
  (:require
    [clojure.test :refer :all]
    [logada.core :as logada]
    [clojure.tools.logging :as log])
  (:import
    (ch.qos.logback.core ConsoleAppender)))

(def main-cfg [:configuration
               [:appender {:name :log.appender/STDOUT, :class ConsoleAppender}
                [:encoder [:pattern "%d %-5p [%c{2}] %m%n"]]]
               [:root {:level :info}
                [:appender-ref {:ref :log.appender/STDOUT}]]
               [:logger {:name 'io.pedestal, :level :info}]
               [:logger {:name 'foo.bar, :level :warn}]])

(logada/gen-xml main-cfg true)


(comment

  (logada/init-logback! main-cfg)

  (log/info "Foo")

  ;; Test:
  (do
    (ns io.pedestal.foo)
    (clojure.tools.logging/debug "Souldn't log")
    (clojure.tools.logging/info "Should log"))

  (do
    (ns foo.bar)
    (clojure.tools.logging/info "SHouldn't log"))

  )
