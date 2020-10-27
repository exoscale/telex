# net-http

Very minimal http(2) client based on jdk11+ HttpClient.

## Approach

We just supply very thin wrappers over client instantiation and
response/request handling. All the heavy lifting is done via an
interceptor chain that is 1st class.

By default it supports ring and ring2 style request/response, but you
could also hit the response/request protocol functions direclty if you
want/need to use a more imperative style (and more gc friendly) api.

By default it will use the ring1 style api:

``` clj
(require '[exoscale.net.http.client :as c])

;; creates a new client instance
(def c (c/client {}))

;; perform request, everything goes through that single function, no (get ...) (post ...), etc.
@(c/request c {:method :get
               :url "http://google.com/"
               :query-params {:foo :bar}
               :form-params {:foo :bar1}})
```

It will use the async interface of the underlying client,
so it will return a CompletableFuture.

You can see the various client options in the
`exoscale.net.http.client.option` ns.

You can also pass additional keys to the context (request map) to alter behavior:

Response options:

* `:exoscale.net.http.client.response/body-handler`
* `:exoscale.net.http.client.response/executor`

Body handler options are in `exoscale.net.http.client.response`, some
of them can also take extra arguments:

- possible values are `:discarding` `:string` `:byte-array`
`:input-stream` `:publisher` `:byte-array-consumer` `:file`
`:file-download` `:subscriber` `:line-subscriber` `:buffering`
`:replacing` `:lines`.

Request options:

* `exoscale.net.http.client.request/async?` (defaults  to true)

Request body is handled via `exoscale.net.http.client.request/BodyPublisher` by default it will handle: bytes, string, input-stream and HttpRequest$BodyPublishers.

## Documentation

[![cljdoc badge](https://cljdoc.xyz/badge/exoscale/net-http)](https://cljdoc.xyz/d/exoscale/net-http/CURRENT)

## Installation

net-http is [available on Clojars](https://clojars.org/exoscale/net-http).

Add this to your dependencies:

[![Clojars Project](https://img.shields.io/clojars/v/exoscale/net-http.svg)](https://clojars.org/exoscale/net-http)

## Usage



## License

Copyright © 2020 [Exoscale](https://exoscale.com)
