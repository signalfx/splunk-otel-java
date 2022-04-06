## Sampling configuration

| System property          | Environment variable     | Default value  | Support | Description |
| ------------------------ | ------------------------ | -------------- | ------- | ----------- |
| `otel.traces.sampler`    | `OTEL_TRACES_SAMPLER`     | `always_on`    | Stable  | The sampler to use for tracing.	|

Splunk Distribution of OpenTelemetry Java supports all standard samplers as provided by
[OpenTelemetry Java SDK Autoconfigure](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#sampler).
In addition, the distribution adds the following samplers:

### `internal_root_off`
Setting `otel.traces.sampler` to `internal_root_off` drops all traces with root spans where `spanKind` is `INTERNAL`, `CLIENT` or `PRODUCER`. This setting only keeps root spans where `spanKind` is `SERVER` and `CONSUMER`.

### `rules`
This sampler allows to ignore individual endpoints and drop all traces that originate from them.
It applies only to spans with `SERVER` kind.

For example, the following configuration results in all requests to `/healthcheck` to be excluded from monitoring:

```shell
export OTEL_TRACES_SAMPLER=rules
export OTEL_TRACES_SAMPLER_ARG=drop=/healthcheck;fallback=parentbased_always_on
```
All requests to downstream services that happen as a consequence of calling an excluded endpoint are also excluded.

The value of `OTEL_TRACES_SAMPLER_ARG` is interpreted as a semicolon-separated list of rules.
The following types of rules are supported:

- `drop=<value>`: The sampler drops a span if its `http.target` attribute has a substring equal to the provided value.
  You can provide as many `drop` rules as you want.
- `fallback=sampler`: Fallback sampler used if no `drop` rule matched a  given span.
  Supported fallback samplers are `always_on` and `parentbased_always_on`.
  Probability samplers such as `traceidratio` are not supported.

> If several `fallback` rules are provided, only the last one will be in effect.

If `OTEL_TRACES_SAMPLER_ARG` is not provided or has en empty value, no `drop` rules are configured and `parentbased_always_on` sampler is as default.
