# telex

Very minimal http(2) client based on jdk11+ HttpClient.

## Approach

We just supply very thin wrappers over client instantiation and
response/request handling. All the heavy lifting is done via an
interceptor chain that is 1st class.

By default it supports ring and ring2 style request/response, but you
could also hit the response/request protocol functions direclty if you
want/need to use a more imperative style (and more gc friendly) api.

## Usage

By default it will use the ring1 style api:

``` clj
(require '[exoscale.telex :as c])

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

Client options (to be passed to the client builder):

* `:exoscale.telex/authenticator`: authenticator - https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/Authenticator.html
* `:exoscale.telex/connect-timeout`: connection timeout duration (in ms)
* `:exoscale.telex/cookie-handler`: cookie-handler - https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/CookieHandler.html
* `:exoscale.telex/executor` : executor to be used for asynchronous and dependent tasks - https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Executor.html
* `:exoscale.telex/follow-redirects`: whether requests will automatically follow redirects issued by the server (`:never`, `:always`, `:normal`)
* `:exoscale.telex/priority`: priority for any HTTP/2 requests sent from this client (number) -
* `:exoscale.telex/proxy`: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/ProxySelector.html
* `:exoscale.telex/ssl-context`: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/net/ssl/SSLContext.html (do yourself an favor an use `less-awful-ssl`)
* `:exoscale.telex/ssl-parameters`: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/net/ssl/SSLParameters.html
* `:exoscale.telex/version`: HTTP protocol version (`:http-1-1`, `:http-2`)

You can also pass additional keys to the context (request map) to alter behavior

Request options:

* `:exoscale.telex.request/async?` (defaults  to true)
* `:exoscale.telex.request/throw-on-error?` (defaults  to true)
* `:exoscale.telex.request/timeout` request timeout (ms)
* `:exoscale.telex.request/version` HTTP protocol version (`:http-1-1`, `:http-2`)
* `:exoscale.telex.request/expect-continue?` Wheter this request's expect continue setting.
* `:exoscale.telex.request/interceptor-chain`: Set custom interceptor chain for request handling


Request body is handled via
`exoscale.telex.request/BodyPublisher` by default it will
handle: bytes, string, input-stream and HttpRequest$BodyPublishers.

For now the docs are almost non-existant but the source is quite
minimal. That will come in time, once the api stabilizes.

Response options:

* `:exoscale.telex.response/body-handler`
* `:exoscale.telex.response/executor`

Body handler options are in `exoscale.telex.response`, some
of them can also take extra arguments:

- possible values are `:discarding` `:string` `:byte-array`
`:input-stream` `:publisher` `:byte-array-consumer` `:file`
`:file-download` `:subscriber` `:line-subscriber` `:buffering`
`:replacing` `:lines`.


## Exceptional http statuses

Exceptional http statuses will cause a throw with an exoscale.ex
anomaly type ex-info. You can see the mapping here: https://github.com/exoscale/ex/blob/master/modules/ex-http/src/clj/exoscale/ex/http.clj.

You can disable that by modifying the interceptor chain used by `request` calls

## Interceptor chain

You will find the following values in the context by default:

* `:exoscale.telex/client` the current client
* `:exoscale.telex.request/interceptor-chain` the full chain

You will also find all the request context and keys from [exoscale.interceptor](https://github.com/exoscale/interceptor)

## Things it doesn't do

Right now we do not aim with clj-http compatibility, it's
intentionally minimalistic, this make it very predictable and low
overhead.  If you need special handling of array params (by default we
repeast the param, no `?a[]=foo&a[]=bar` handling), multipart upload
and these kind things you have to create interceptors yourself to do
it, it's quite easy, most libraries that did that just copy some of
the helper namespaces from clj-http, we'd suggest you'd do that on a
case by case basis.

## ring2 (experimental)

RING2 is not yet stable as a spec, but you can use
`exoscale.telex/request2` to give it a try. It will return
namespaced keys and change a few original keys from ring1. headers
handling is identical to ring1 for now, it's not yet clear what ring2
will settle on (it's under discussion still). The async part of the
spec is also intentionally ignored for now since the jdk client relies
on Flow & CompletableFuture.

https://github.com/ring-clojure/ring/blob/2.0/SPEC-2.md

## Documentation

[![cljdoc badge](https://cljdoc.xyz/badge/exoscale/telex)](https://cljdoc.xyz/d/exoscale/telex/CURRENT)

## Installation

telex is [available on Clojars](https://clojars.org/exoscale/telex).

Add this to your dependencies:

[![Clojars Project](https://img.shields.io/clojars/v/exoscale/telex.svg)](https://clojars.org/exoscale/telex)

## License

Copyright © 2020 [Exoscale](https://exoscale.com)
