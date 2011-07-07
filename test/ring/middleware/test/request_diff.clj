(ns ring.middleware.test.request-diff
  (:use ring.middleware.request-diff :reload)
  (:use clojure.test)
  (:use (ring.middleware (params :only [wrap-params])
                         (keyword-params :only [wrap-keyword-params]))
        (ring.mock (request :only [request])))
  (:require [clojure.string :as string]))

;; ## Testing Utility Functions

(defn- collapse-whitespace
  "Normalize strings by replacing consecutive stretches of whitespace (except newlines) with a single space."
  [s]
  (string/trim (string/replace s #"[ \t\x0B\f\r]+" " ")))

(deftest test-collapse-whitespace
  (is (= (collapse-whitespace " foo\n\t$bar-baz      ")
         "foo\n $bar-baz")))

(defn- anonymize-object-ids
  "Replace the hex string object id in the printed representation of a Java class instance with `\"XXXXXXXX\"`"
  [s]
  (string/replace s #"@[0123456789abcdef]+>" "@XXXXXXXX>"))

(deftest test-anonymize-object-ids
  (is (= (anonymize-object-ids "Diff /foo/bar for :params with #<params$wrap_params ring.middleware.params$wrap_params@576ba7f9> - nil + {}")
         "Diff /foo/bar for :params with #<params$wrap_params ring.middleware.params$wrap_params@XXXXXXXX> - nil + {}")))

(defn- wrapped-and-unwrapped
  "Given a middleware function, create both a `:params`-monitoring, `request-diff`-wrapped handler and a non-wrapped handler"
  [middleware-fn]
  [(wrap-request-diff identity [:params] middleware-fn)
   (middleware-fn identity)])

(defn- capture-logging
  "Run `req` through `handler`, capture any logged output, and normalize it for testing purposes"
  [req handler]
  (-> req handler with-out-str collapse-whitespace anonymize-object-ids))

;; ## Middleware Tests

(deftest no-adverse-side-effects
  (let [[diff-handler handler] (wrapped-and-unwrapped wrap-params)]
    (are [req] (= (diff-handler req) (handler req))
         (request :get "/foo/bar")      ;
         (request :get "/foo/bar" {"x" 5})))
  (let [[diff-handler handler] (wrapped-and-unwrapped wrap-keyword-params)]
    (are [req] (= (diff-handler req) (handler req))
         (request :get "/foo/bar")
         (assoc (request :get "/foo/bar" {"x" 5})
           :params {"x" 5}))))

(deftest diff-strings-printing
  (testing "No changes observed"
    (let [[handler _] (wrapped-and-unwrapped identity)]
      (are [req output] (= (capture-logging req handler)
                           output)

           (request :get "/foo/bar")
           "No difference on /foo/bar for :params with #<core$identity clojure.core$identity@XXXXXXXX>")))

  (testing "Basic diff observation"
    (let [[handler _] (wrapped-and-unwrapped wrap-params)]
      (are [req output] (= (capture-logging req handler)
                           output)
           (request :get "/foo/bar")
           "Diff /foo/bar for :params with #<params$wrap_params ring.middleware.params$wrap_params@XXXXXXXX>\n - nil\n + {}"

           (request :get "/foo/bar" {"x" 5})
           "Diff /foo/bar for :params with #<params$wrap_params ring.middleware.params$wrap_params@XXXXXXXX>\n - nil\n + {\"x\" \"5\"}"))

    (let [[handler _] (wrapped-and-unwrapped wrap-keyword-params)]
      (are [req output] (= (capture-logging req handler)
                           output)

           (request :get "/foo/bar")
           "No difference on /foo/bar for :params with #<keyword_params$wrap_keyword_params ring.middleware.keyword_params$wrap_keyword_params@XXXXXXXX>"

           (assoc (request :get "/foo/bar" {"x" 5})
             :params {"x" "5"})
           "Diff /foo/bar for :params with #<keyword_params$wrap_keyword_params ring.middleware.keyword_params$wrap_keyword_params@XXXXXXXX>\n {\n - \"\n + :\n x\n - \"\n \"5\"}"))))

(run-tests)
