# Micrometer metrics in the Splunk distro

This short doc aims to describe the current (legacy) metrics solution in the Splunk distro: how we use Micrometer, which
parts are placed where (and in which class loader), and how they interact.

Since we're currently not using the OpenTelemetry metrics API & SDK (this will change in the future), we're disabling
the OTel metrics exporter and all known metrics instrumentations by default in the `SplunkConfiguration` class.

The `micrometer-core` library is used as a metrics API, and it's placed in the `bootstrap` module; and thus loaded by
the bootstrap class loader. To make sure that we do not conflict with the Micrometer version that might be included in
the instrumented application, we shade all micrometer classes. The agent (and all metrics instrumentations that we have)
rely on the global `Metrics.globalRegistry` meter registry (kind of analogous to how we're using `GlobalOpenTelemetry`).
The `MicrometerBootstrapPackagesProvider` SPI implementation (in the `custom` module, since it's an agent class) ensures
that all `micrometer-core` classes will always be loaded by the bootstrap class loader.

The actual SignalFx registry/exporter (from `micrometer-registry-signalfx`) is added as a dependency to the `custom`
module, and thus loaded by the agent class loader. The `MicrometerInstaller` class (`BeforeAgentListener` SPI
implementation) builds the meter registry configuration, and if metrics are enabled, installs the newly
created `SignalFxMeterRegistry` in the `Metrics.globalRegistry`, so that it's visible from all classloaders (and all
metrics instrumentations).

Micrometer metrics is an experimental feature, and is not enabled by default: you have to manually enable it.

We have several metrics instrumentations (metric instruments are described in more detail
in [the main metrics doc](../metrics.md)): almost all of them use `MetricsInstrumentationModule` as the base class (
which disables the instrumentation if metrics are not enabled), the only exception being the JVM metrics
instrumentation, which uses a custom `AgentListener` to install itself.

To make it possible to add custom metrics from the instrumented application, the Splunk distro includes a Micrometer
bridge instrumentation. It utilizes a similar strategy as the upstream `opentelemetry-api` bridge, that is we have a
separate "application" shaded Micrometer (in `:instrumenter:micrometer-1.5-shaded-for-instrumenting`) that prefixes all
the class names with `application.`. This shaded Micrometer is only used to compile the bridge instrumentation; the
prefix is removed when the agent is assembled. The bridge instrumentation itself is fairly straightforward (even if it's
a lot of code): it translates all calls to the application `MeterRegistry` to our own `MeterRegistry` (the shaded one
present in the bootstrap). The combination of shading + muzzle ensures the safety of the bridge instrumentation; it
won't be installed in applications containing an incompatible (old, or modified) Micrometer version.
