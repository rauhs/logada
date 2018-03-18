(ns logada.core
  "
  Configure logback logging by using hiccup instead of XML.

  Uses the exact same structure as the XML.
  "
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str])
  (:import
    [org.slf4j LoggerFactory]
    [ch.qos.logback.classic LoggerContext]
    [ch.qos.logback.core.util StatusPrinter]
    [ch.qos.logback.classic.joran JoranConfigurator]
    (java.net URL)
    (javax.xml.parsers DocumentBuilderFactory)
    (org.w3c.dom Document Element)
    (javax.xml.transform.stream StreamResult)
    (javax.xml.transform.dom DOMSource)
    (javax.xml.transform OutputKeys TransformerFactory)
    (java.io StringWriter ByteArrayInputStream)
    (java.nio.charset StandardCharsets)))

(defn- load-logback
  "Loads a logback.xml file for configuration.

  (load-logack (io/resource \"logback.xml\"))"
  [^URL logback-xml-file]
  (let [context (LoggerFactory/getILoggerFactory)
        configurator (doto (JoranConfigurator.)
                       (.setContext context))]
    (.reset ^LoggerContext context)
    (.doConfigure configurator logback-xml-file)
    (StatusPrinter/printInCaseOfErrorsOrWarnings context)
    (log/info "Loaded logback configuration" (str logback-xml-file))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- write-str
  "Writes an XML string from the an XML node"
  ^String [node pretty?]
  (let [sw (StringWriter.)
        tf (.newTransformer (TransformerFactory/newInstance))]
    (doto tf
      (.setOutputProperty OutputKeys/OMIT_XML_DECLARATION "yes")
      (.setOutputProperty OutputKeys/INDENT (if pretty? "yes" "no")))
    (.transform tf (DOMSource. node) (StreamResult. sw))
    (.toString sw)))

(defn camel-case
  "Kebab to camelcase. Ignores the namespace part."
  ^String [kw]
  (->> (str/split (name kw) #"-")
       (map str/capitalize)
       (str/join "")))
#_(camel-case :foo/bar-baz)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main API:
(defmulti transform-attr-value
          "Transforms attribute values of the XML tags"
          class)
;; This allows use to say {:class FileAppender}
(defmethod transform-attr-value java.lang.Class [^Class klass] (.getName klass))
(defmethod transform-attr-value :default [kw] kw)

(def cc-attr-keys
  "CamelCase-able attribute keys. Attributes in this set will be camelcased."
  (atom #{:date-pattern
          :packaging-data
          :time-reference}))

(defn- attr-key->str
  "Translates attribute keys of the XML tags to strings."
  [attr-key]
  (let [ccable @cc-attr-keys]
    (if (ccable attr-key)
      (camel-case attr-key)
      (name attr-key))))

(def cc-tags
  "CamelCase-able tags. Tags in this set will be camelcased."
  (atom #{:date-pattern
          :time-reference
          :on-mismatch
          :on-match
          :immediate-flush
          :context-ame
          :context-listener
          :insert-from-JNDI
          :jmx-configurator
          :console-plugin
          :reset-JUL
          :substitution-property
          :shutdown-hook
          :context-property
          :conversion-rule
          :status-listener
          :max-file-size
          :min-index
          :max-index
          :rolling-policy
          :total-size-cap
          :max-history
          :file-name-pattern
          :new-rule}))

(defn possibly-rename-tag
  "Can rename some tags to accept more clojure like configuration
  [:immediate-flush ...] instead of
  [:immediateFlush ...].

  Note: We can't just map all kebab case to camelcase since some
  tags are actually kebab case."
  [tag]
  (let [ccable @cc-tags]
    (if (ccable tag)
      (camel-case tag)
      tag)))

(defmulti stringify-val
          "Translates
          - tag content and
          - attribute values
          to strings."
          class)
(defmethod stringify-val clojure.lang.Keyword [x] (name x))
(defmethod stringify-val :default [x] (str x))

(defn- elem
  "Inserts an XML element into the XML document"
  [^Document doc ^Element parent tag attrs & children]
  (let [el (.createElement doc (name (possibly-rename-tag tag)))
        text! #(.appendChild el (.createTextNode doc (str/trim (stringify-val %))))]
    (cond
      (map? attrs) (doseq [[k v] attrs]
                     (.setAttribute el (attr-key->str k)
                                    (stringify-val (transform-attr-value v))))
      (vector? attrs) (.appendChild el (apply elem doc el attrs))
      :else (text! attrs))
    (doseq [child children]
      (if (vector? child)
        (.appendChild el (apply elem doc el child))
        (text! child)))
    (.appendChild parent el)
    el))

(defn gen-xml
  "Generates an XML string from hiccup (clojure data)"
  ^String [src pretty?]
  (let [xml-builder (.newDocumentBuilder (DocumentBuilderFactory/newInstance))
        doc (.newDocument xml-builder)]
    (apply elem doc doc src)
    (write-str doc pretty?)))

(defn get-last-statuses
  "Hack, similar to StatusPrinter/printInCaseOfErrorsOrWarnings but returns a vec of strings"
  [^LoggerContext context ms]
  (when-let [sm (.getStatusManager context)]
    (let [now-1s (- (System/currentTimeMillis) ms)]
      (into []
            (comp (map bean)
                  (filter (comp #(< now-1s %) :date))
                  (map :message))
            (.getCopyOfStatusList sm)))))
#_(get-last-statuses (LoggerFactory/getILoggerFactory) 500000000)

(defn init-logback!
  "Inits logback logger given hiccup data (same format as XML)
  Resets any existing configuration and closes all existing appenders!

  Returns a vector of message that shows what the configurator saw."
  [hiccup]
  (let [context ^LoggerContext (LoggerFactory/getILoggerFactory)
        configurator (doto (JoranConfigurator.)
                       (.setContext context))
        xml (gen-xml hiccup false)]
    (.reset context)
    (.doConfigure configurator
                  (ByteArrayInputStream. (.getBytes xml StandardCharsets/UTF_8)))
    (.start context)
    ;; This prints to REPL... :/
    #_(StatusPrinter/printInCaseOfErrorsOrWarnings context)
    ;; Return the status messages:
    (get-last-statuses context 1000)))

