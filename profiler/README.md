
> :construction: The profiler feature is experimental.

# About the Java profiler

The `splunk-otel-java` agent includes a continuous profiler that can be enabled with a configuration
setting. This profiler periodically captures the call stack state for all JVM threads and
sends these to the Splunk Observability Cloud. You can then view a flamegraph of application
call stacks and inspect individual code-level call stacks for relevant traces.

## How does the profiler work?

The profiler leverages the JVM's JDK Flight Recorder (JFR) to perform periodic call
stack sampling. For every recording period, a new JFR recording file is flushed to disk and events are replayed.

In order to associate `jdk.ThreadDump` events from JFR with OpenTelemetry spans, a custom
OpenTelemetry `ContextStorage` implementation is used to emit `otel.ContextAttached`
events each time a span context change occurs. These context changes are then tracked
during replay so that a call stack can be associated with the appropriate span.

Stack trace data is embedded as a string inside of an OTLP logs payload. The
[Splunk OpenTelemetry Connector](https://github.com/signalfx/splunk-otel-collector)
will detect this profiling data inside of OTLP logs and will help it along
its ingest path.

# Requirements

* Java 8 above 8u262, or Java 11+
* 100 MB of free disk space
* [Splunk OpenTelemetry Connector](https://github.com/signalfx/splunk-otel-collector) version 0.33.1 or higher. _Sending profiling data directly to ingest is not supported at this time_.
* Profiler is enabled at startup (disabled by default, see the Configuration section)

# Enable the profiler

To enable the profiler, pass the `-Dsplunk.profiler.enabled=true` argument to your JVM or set the `SPLUNK_PROFILER_ENABLED` environment variable to your JVM process.

# Configuration settings

Like all other OpenTelemetry Java configurations, the following settings can be used as a system
property or as environment variables (by making them uppercase and replacing dots with underscores).

> We strongly recommend using defaults for the following settings.

| Setting                                  | Default                | Description                               |
|------------------------------------------|------------------------|-------------------------------------------|
|`splunk.profiler.enabled`                 | false                  | set to true to enable the profiler        |
|`splunk.profiler.directory`               | "."                    | location of jfr files                     |
|`splunk.profiler.recording.duration`      | 20s                    | recording unit duration                   |
|`splunk.profiler.keep-files`              | false                  | leave JFR files on disk id `true`         |
|`splunk.profiler.logs-endpoint`           | `otel.exporter.otlp.endpoint` or http://localhost:4317  | where to send OTLP logs                   |
|`splunk.profiler.call.stack.interval`     | 10000ms                | how often to sample call stacks           |
|`splunk.profiler.memory.enabled`          | false                  | set to `true` to enable all other memory profiling options unless explicitly disabled |
|`splunk.profiler.tlab.enabled`            | `splunk.profiler.memory.enabled` | set to `true` to enable TLAB events even if `splunk.profiler.memory.enabled` is `false` |
|`splunk.profiler.memory.sampler.interval` | 1                      | set to `2` or larger to enable sampling every Nth allocation event where N is the value of this property |
|`splunk.profiler.include.internal.stacks` | false                  | set to `true` to include stack traces of agent internal threads and stack traces with only JDK internal frames |
|`splunk.profiler.tracing.stacks.only`     | false                  | set to `true` to include only stack traces that are linked to a span context |

If the `splunk.profiler.enabled` option is not enabled, all profiling features are disabled. For
example, setting `splunk.profiler.memory.enabled` to `true` has no effect if
`splunk.profiler.enabled` is set to `false`. Similarly, there is no separate toggle for periodic
collection of call stacks (thread dumps), as this feature is enabled only when the profiler is
enabled.

# Escape hatch

The profiler limits its own behavior under two conditions:

* Free disk space is too low (< 100MB free)
* More than 5 minutes worth of JFR files are backed up on disk

If a recording is already running when this condition is met, the recording stops. When
the conditions are no longer applicable, the recording and the profiler resume operation.

# FAQ / Troubleshooting

### How do I know if it's working?

At startup, the agent will log the string "JFR profiler is active" at `INFO`. You can grep for this in your logs to see
something like this:
```
[otel.javaagent 2021-09-28 18:17:04:246 +0000] [main] INFO com.splunk.opentelemetry.profiler.JfrActivator - JFR profiler is active.
```

### How can I see the profiler configuration?

The agent logs the profiling configuration at `INFO` during startup. You can grep for the string
`com.splunk.opentelemetry.profiler.ConfigurationLogger` to see entries like the following:

```
[otel.javaagent 2021-09-28 18:17:04:237 +0000] [main] INFO <snip> - -----------------------
[otel.javaagent 2021-09-28 18:17:04:237 +0000] [main] INFO <snip> - Profiler configuration:
[otel.javaagent 2021-09-28 18:17:04:238 +0000] [main] INFO <snip> -                 splunk.profiler.enabled : true
[otel.javaagent 2021-09-28 18:17:04:239 +0000] [main] INFO <snip> -               splunk.profiler.directory : .
[otel.javaagent 2021-09-28 18:17:04:244 +0000] [main] INFO <snip> -      splunk.profiler.recording.duration : 20s
[otel.javaagent 2021-09-28 18:17:04:244 +0000] [main] INFO <snip> -              splunk.profiler.keep-files : false
[otel.javaagent 2021-09-28 18:17:04:245 +0000] [main] INFO <snip> -           splunk.profiler.logs-endpoint : null
[otel.javaagent 2021-09-28 18:17:04:245 +0000] [main] INFO <snip> -             otel.exporter.otlp.endpoint : http://collector:4317
[otel.javaagent 2021-09-28 18:17:04:245 +0000] [main] INFO <snip> -            splunk.profiler.tlab.enabled : false
[otel.javaagent 2021-09-28 18:17:04:246 +0000] [main] INFO <snip> -   splunk.profiler.period.jdk.threaddump : null
[otel.javaagent 2021-09-28 18:17:04:246 +0000] [main] INFO <snip> - -----------------------
```

### What about this escape hatch?

If the escape hatch becomes active, it will log with `com.splunk.opentelemetry.profiler.RecordingEscapeHatch`
(you can grep for this in the logs). You may also look for `"** THIS WILL RESULT IN LOSS OF PROFILING DATA **"`
as a big hint that things are not well.

You may need to free up some disk space and/or give the JVM more resources.

### What if I'm on an unsupported JVM?

If your JVM does not support JFR, the profiler logs a warning at startup with the
message `JDK Flight Recorder (JFR) is not available in this JVM. Profiling is disabled`.
If you want to use the profiler and see this in your logs, you must upgrade
your JVM version to 8u262 or later.

### Why is the OTLP/logs exporter complaining?

Collector configuration issues may prevent logs from being exported and profiling data from showing in Splunk Observability Cloud.

Check for the following common issues:

* Look at the values of the agent's configuration: `splunk.profiler.logs-endpoint` and `otel.exporter.otlp.endpoint`. Hint: they are logged
at startup (see above).
* Verify that a collector is actually running at that endpoint and that the
application host/container can resolve any hostnames and actually connect to the given OTLP port (default: 4317)
* Make sure you are running the [Splunk OpenTelemetry Connector](https://github.com/signalfx/splunk-otel-collector)
and that the version is 0.33.1 or greater. Other collector distributions may not be able to route the log
data containing profiles correctly.
* Make sure that the collector is configured correctly to handle profiling data. By default, the
[Splunk OpenTelemetry Connector](https://github.com/signalfx/splunk-otel-collector) handles this, but
a custom configuration might have overridden some settings. Make sure that an OTLP _receiver_ is configured in the collector
and that an exporter is configured for `splunk_hec` export. Ensure that the `token` and `endpoint` fields
are [correctly configured](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/splunkhecreceiver#configuration).
Lastly, double check that the logs _pipeline_ is configured to use the OTLP receiver and `splunk_hec` exporter.

### Can I tell the agent to ignore some threads?

Not yet. Some JVM internal threads are automatically omitted, but there is no user-serviceable mechanism
for omitting or filtering threads. If you need this, file an issue.
