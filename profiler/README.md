
The profiler is in its infancy and is disabled by default.

It should be considered experimental and is completely unsupported.

# configuration

| name                                | default                | description                               |
|-------------------------------------|------------------------|-------------------------------------------|
|`splunk.profiler.enabled`            | false                  | set to true to enable the profiler        |
|`splunk.profiler.directory`          | "."                    | location of jfr files                     |
|`splunk.profiler.recording.duration` | 20s                    | recording unit duration                   |
|`splunk.profiler.keep-files`         | false                  | leave JFR files on disk id `true`         |
|`splunk.profiler.logs-endpoint`      | `otel.exporter.otlp.endpoint` or http://localhost:4317  | where to send OTLP logs                   |
|`splunk.profiler.period.{eventName}` | n/a                    | customize period (in ms) for a specific jfr event. For example, to set the ThreadDump frequency to 1s (100ms): `-Dsplunk.profiler.period.threaddump=1000` |
|`splunk.profiler.tlab.enabled`       | true                   | set to `false` to disable TLAB memory events |
