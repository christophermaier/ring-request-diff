# ring-request-diff

See how Ring middleware changes a request.

## Usage

`ring-request-diff` wraps other Ring middleware, allowing you to monitor how that middleware changes the values of specific keys of a request map.

    (-> #'handler
        ring.middleware.nested-params/wrap-nested-params
        (wrap-request-diff [:params] ring.middleware.keyword-params/wrap-keyword-params)
        ring.middleware.params/wrap-params)

It takes a vector of request keys (keyword or string form; either one works), the middleware function it wraps, and any additional arguments required for that wrapped function.

The request is run through the wrapped middleware function.  Whenever the value stored under one of the watched keys is changed, a diff of the value (pre- vs. post-middleware) is printed to `*out*`.  The processed request is then sent down the rest of the middleware chain.

This comes in handy for debugging complex stacks of middleware, allowing you to easily see exactly what each function is doing, making sure the ordering of middleware is correct, etc.

## Install

Add the following dependency to your `project.clj` file:

    [ring-request-diff "0.1.0"]

## License

Copyright (C) 2011 Christopher Maier

Distributed under the Eclipse Public License, the same as Clojure.
