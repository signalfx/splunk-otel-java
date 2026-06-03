/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.opamp;

import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.ProfilerEnvVarsConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingEnvVarsConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

class EnvVarsEffectiveConfigFileFactory implements EffectiveConfigFactory {
  private final ConfigProperties config;

  EnvVarsEffectiveConfigFileFactory(ConfigProperties config) {
    this.config = config;
  }

  @Override
  public String getContentType() {
    return "text/plain; format=properties; vendor=splunk; v=1.0.0";
  }

  @Override
  public String getFileName() {
    return "environment";
  }

  public String createEffectiveConfigContent() {
    EffectiveConfigBuilder builder = new EffectiveConfigBuilder();

    addOtelEnvVars(builder);
    addSplunkEnvVars(builder);

    return builder.build();
  }

  private void addSplunkEnvVars(EffectiveConfigBuilder builder) {
    ProfilerConfiguration profilerConfiguration = new ProfilerEnvVarsConfiguration(config);
    SnapshotProfilingConfiguration snapshotConfiguration =
        new SnapshotProfilingEnvVarsConfiguration(config);

    builder
        .add("SPLUNK_PROFILER_ENABLED", profilerConfiguration.isEnabled())
        .add("SPLUNK_PROFILER_MEMORY_ENABLED", profilerConfiguration.getMemoryEnabled())
        .add("SPLUNK_SNAPSHOT_PROFILER_ENABLED", snapshotConfiguration.isEnabled())
        .add(
            "SPLUNK_SNAPSHOT_PROFILER_SAMPLING_INTERVAL",
            snapshotConfiguration.getSamplingInterval())
        .add("SPLUNK_PROFILER_CALL_STACK_INTERVAL", profilerConfiguration.getCallStackInterval());
  }

  private void addOtelEnvVars(EffectiveConfigBuilder builder) {
    builder
        .add("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", getSignalEndpoint(config, "traces"))
        .add("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", getSignalEndpoint(config, "metrics"))
        .add("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT", getSignalEndpoint(config, "logs"))
        .add("OTEL_CONFIG_FILE", (String) null)
        .add("OTEL_EXPERIMENTAL_CONFIG_FILE", (String) null);
  }

  private static String getSignalEndpoint(ConfigProperties config, String signal) {
    // If otlp exporter is not enabled for given signal then return an empty string
    String exporterPropertyName = "otel." + signal + ".exporter";
    if (!"otlp".equals(config.getString(exporterPropertyName, "otlp"))) {
      return "";
    }

    // Attempt to get specified endpoint for given signal
    String propertyName = "otel.exporter.otlp." + signal + ".endpoint";
    String endpoint = config.getString(propertyName);
    if (endpoint != null) {
      return endpoint;
    }

    // If no endpoint specified explicitly then deduce from the default endpoint and protocol used
    String baseEndpoint = config.getString("otel.exporter.otlp.endpoint");
    boolean isGrpc = "grpc".equals(getSignalOtlpProtocol(config, signal));
    if (baseEndpoint == null) {
      return isGrpc ? "http://localhost:4317" : "http://localhost:4318/v1/" + signal;
    }
    if (!isGrpc) {
      return appendSignalPath(baseEndpoint, signal);
    }
    return baseEndpoint;
  }

  private static String getSignalOtlpProtocol(ConfigProperties config, String signal) {
    String propertyName = "otel.exporter.otlp." + signal + ".protocol";
    return config.getString(propertyName, getOtlpProtocol(config));
  }

  private static String getOtlpProtocol(ConfigProperties config) {
    return config.getString("otel.exporter.otlp.protocol", "http/protobuf");
  }

  private static String appendSignalPath(String endpoint, String signal) {
    String signalPath = "v1/" + signal;
    if (endpoint.endsWith(signalPath)) {
      return endpoint;
    }
    if (!endpoint.endsWith("/")) {
      endpoint += "/";
    }
    return endpoint + signalPath;
  }
}
