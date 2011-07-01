(ns ring.middleware.request-diff
  "See how middleware changes a Ring request body"
  (:use (com.georgejahad (difform :only [difform]))))

(defn wrap-request-diff
  "Debug other middleware by wrapping them and printing out what they change in a Ring request body.

  `keys` is a sequence of request keys (string or keyword) that you want to monitor changes in.
  `middleware-fn` is the middleware function being wrapped
  `args` (optional) are for `middleware-fn`, and will be passed through to it."
  [handler keys middleware-fn & args]
  (letfn [(omni-get [m k] ;; handle either keyword or string parameter names
            (or (get m (keyword k))
                (get m (name k))))]
    (fn [req]
      (let [processed-req ((apply middleware-fn identity args) req)]
        (doseq [k keys]
          (let [old-part (omni-get req k)
                new-part (omni-get processed-req k)
                uri (omni-get req :uri)]
            (if-not (= old-part new-part)
              (do (println "Diff" uri "for" k "with" middleware-fn)
                  (difform old-part new-part))
              (println "No difference on" uri "for" k "with" middleware-fn))))
        (handler processed-req)))))
