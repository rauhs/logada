# Logada

A simple library to configure Logback logging without the need for
a `logback.xml`.

The config is instead done with Hiccup from your clojure code:

```clojure
(ns your-ns.core
  (:require
    [logada.core :as logada]
    [clojure.tools.logging :as log])
  (:import
    (ch.qos.logback.core ConsoleAppender)))
    
(logada/init-logback!
  [:configuration
    [:appender {:name :log.appender/STDOUT, :class ConsoleAppender}
     [:encoder [:pattern "%d %-5p [%c{2}] %m%n"]]]
    [:root {:level :info}
     [:appender-ref {:ref :log.appender/STDOUT}]]
    [:logger {:name 'io.pedestal, :level :info}]
    [:logger {:name 'foo.bar, :level :warn}]])
``` 

# Dependencies

It has no dependencies. You should require the libs you want by yourself.

Example:

```clojure
[[org.clojure/clojure "1.9.0"]
 [org.clojure/tools.logging "0.4.0"]
 [ch.qos.logback/logback-classic "1.2.3" :exclusions [[org.slf4j/slf4j-api]]]
 [org.slf4j/jul-to-slf4j "1.7.25"]
 [org.slf4j/jcl-over-slf4j "1.7.25"]
 [org.slf4j/log4j-over-slf4j "1.7.25"]]
```

# Clojars

[![Clojars Project](http://clojars.org/logada/latest-version.svg)](http://clojars.org/logada)

# LICENSE

EPL, same as Clojure
