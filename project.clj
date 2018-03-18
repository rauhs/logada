;; For Java-9:
;; :jvm-opts ["--add-modules" "java.xml.bind"]
(defproject logada "0.1.0"
  :description "Clojure Logback logging config with hiccup"
  :url "https://github.com/rauhs/logada"
  ;:uberjar-name "logada-uberjar.jar"
  ;:jvm-opts []
  :javac-options ["-target" "1.8" "-source" "1.6"]
  :jar-exclusions [#"\.swp|\.swo"]
  ;; Paths
  :resource-paths ["resources"]
  :source-paths ["src"]
  :test-paths ["test/clj"]
  :target-path "target/%s/"

  ;; Directory in which to place AOT-compiled files. Including %s will
  ;; splice the :target-path into this value.
  :compile-path "%s/class-files"

  :clean-targets ^{:protect false} ["target"]
  :pedantic? true
  :profiles
  {:test
   {:dependencies
    [[org.clojure/clojure "1.9.0"]
     [org.clojure/tools.logging "0.4.0"]
     [ch.qos.logback/logback-classic "1.2.3" :exclusions [[org.slf4j/slf4j-api]]]
     ;; java.util.logging to slf4j
     [org.slf4j/jul-to-slf4j "1.7.25"]
     ;; Jakarta Commons Logging over slf4j
     [org.slf4j/jcl-over-slf4j "1.7.25"]
     [org.slf4j/log4j-over-slf4j "1.7.25"]]}})
