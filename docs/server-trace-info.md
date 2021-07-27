> The official Splunk documentation for this page is [Server trace information](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/configuration/advanced-java-otel-configuration.html#server-trace-information).

# Server trace information

> :construction: &nbsp;Status: Experimental

Setting `SPLUNK_TRACE_RESPONSE_HEADER_ENABLED` to `true` results in the
following headers being added to HTTP responses produced by the
instrumented application:

```
Access-Control-Expose-Headers: Server-Timing
Server-Timing: traceparent;desc="00-<serverTraceId>-<serverSpanId>-01"
```

The `Server-Timing` header contains the trace information (`traceId` and `spanId`)
in the `traceparent` format. This information is later consumed by the
[splunk-otel-js-web](https://github.com/signalfx/splunk-otel-js-web) library.

Also see the following documents for more information about `Server-Timing` header:

* https://www.w3.org/TR/server-timing/
* https://www.w3.org/TR/trace-context/#traceparent-header

## Frameworks and libraries

The following server frameworks and libraries add `Server-Timing` information:

| Framework/Library | Versions                            |
|-------------------|-------------------------------------|
| Servlet API       | 2.2-4.X (5.0+ is not supported yet) |
| Netty             | 3.8-4.0 (4.1+ is not supported yet) |
