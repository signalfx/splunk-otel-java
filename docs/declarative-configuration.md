# Declarative Configuration

The Splunk Java agent aligns with the [GDI specification](https://github.com/signalfx/gdi-specification/blob/main/specification/configuration.md#file-based-configuration).
In areas where deviations are allowed, it MAY differ from GDI recommendations or from agents for other platforms.

## Profiling configuration

Additional profiling options specific to Java are available under `distribution.splunk.profiling`.
The following YAML shows the complete supported structure:

```yaml
distribution:
  splunk:
    profiling:
      exporter:
        otlp_log_grpc:                # Allowed values: otlp_log_http, otlp_log_grpc
          endpoint: "http://localhost:4317" # SPLUNK_PROFILER_LOGS_ENDPOINT (default: http://localhost:4318/v1/logs or http://localhost:4317 for grpc)

      always_on:
        include_agent_internals: true # SPLUNK_PROFILER_INCLUDE_AGENT_INTERNALS (default: false)
        include_jvm_internals: true   # SPLUNK_PROFILER_INCLUDE_JVM_INTERNALS   (default: false)
        stack_depth: 2048             # SPLUNK_PROFILER_MAX_STACK_DEPTH         (default: 1024)
        keep_recording_files: true    # SPLUNK_PROFILER_KEEP_FILES              (default: false)
        recording_directory: "..."    # SPLUNK_PROFILER_DIRECTORY     (default: system temp dir)
        recording_duration: 10000     # SPLUNK_PROFILER_RECORDING_DURATION      (default: 20000)
        tracing_stacks_only: true     # SPLUNK_PROFILER_TRACING_STACKS_ONLY     (default: false)
        report_locks: true            # SPLUNK_PROFILER_LOCKS_ENABLED           (default: false)

        cpu_profiler:               # SPLUNK_PROFILER_ENABLED
          mode: java
          sampling_interval: 1000   # SPLUNK_PROFILER_CALL_STACK_INTERVAL       (default: 10000)
        memory_profiler:            # SPLUNK_PROFILER_MEMORY_ENABLED
          event_rate: "200/s"       # SPLUNK_PROFILER_MEMORY_EVENT_RATE         (default: 150/s)
          native_sampling: true     # SPLUNK_PROFILER_MEMORY_NATIVE_SAMPLING   (default: false)

      callgraphs:                   # SPLUNK_SNAPSHOT_PROFILER_ENABLED
        sampling_interval: 5        # SPLUNK_SNAPSHOT_SAMPLING_INTERVAL         (default: 10)
        export_interval: 2000       # SPLUNK_PROFILER_EXPORT_INTERVAL           (default: 5000)
        selection_probability: 0.5  # SPLUNK_SNAPSHOT_SELECTION_PROBABILITY     (default: 0.01)
        report_locks: true          # SPLUNK_SNAPSHOT_PROFILER_LOCKS_ENABLED    (default: false)
        staging_capacity: 1000      # SPLUNK_SNAPSHOT_PROFILER_STAGING_CAPACITY (default: 2000)
        stack_depth: 2048           # SPLUNK_SNAPSHOT_PROFILER_MAX_STACK_DEPTH  (default: 1024)
```
