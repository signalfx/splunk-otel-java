
The profiler is in its infancy and is disabled by default.

It should be considered experimental and is completely unsupported.

# configuration

| name                                | default | description                          |
|-------------------------------------|---------|--------------------------------------|
|`splunk.profiler.enabled`            | false   | set to true to enable the profiler   |
|`splunk.profiler.directory`          | "."     | location of jfr files                |
|`splunk.profiler.recording.duration` | 20      | number of seconds per recording unit |
|`splunk.profiler.keep-files`         | false   | leave JFR files on disk id `true`    |
