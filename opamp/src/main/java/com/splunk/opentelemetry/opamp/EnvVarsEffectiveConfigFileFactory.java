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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.ProfilerEnvVarsConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingEnvVarsConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import okio.ByteString;
import opamp.proto.AgentConfigFile;

class EnvVarsEffectiveConfigFileFactory implements EffectiveConfigFactory {
  private static final String SPLUNK_PROFILER_ENABLED = "SPLUNK_PROFILER_ENABLED";
  private static final String SPLUNK_PROFILER_MEMORY_ENABLED = "SPLUNK_PROFILER_MEMORY_ENABLED";
  private static final String SPLUNK_PROFILER_CALL_STACK_INTERVAL =
      "SPLUNK_PROFILER_CALL_STACK_INTERVAL";
  private static final String SPLUNK_SNAPSHOT_PROFILER_ENABLED = "SPLUNK_SNAPSHOT_PROFILER_ENABLED";
  private static final String SPLUNK_SNAPSHOT_PROFILER_SAMPLING_INTERVAL =
      "SPLUNK_SNAPSHOT_PROFILER_SAMPLING_INTERVAL";
  private static final String OTEL_EXPORTER_OTLP_TRACES_ENDPOINT =
      "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT";
  private static final String OTEL_EXPORTER_OTLP_METRICS_ENDPOINT =
      "OTEL_EXPORTER_OTLP_METRICS_ENDPOINT";
  private static final String OTEL_EXPORTER_OTLP_LOGS_ENDPOINT = "OTEL_EXPORTER_OTLP_LOGS_ENDPOINT";

  private static final String OTEL_EXPORTER_OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";
  private static final String OTEL_EXPORTER_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
  private static final String OTLP_PROTOCOL_HTTP_PROTOBUF = "http/protobuf";
  private static final String OTLP_PROTOCOL_GRPC = "grpc";
  private static final String OTLP_SIGNAL_LOGS = "logs";
  private static final String OTLP_SIGNAL_METRICS = "metrics";
  private static final String OTLP_SIGNAL_TRACES = "traces";

  private final ConfigProperties config;

  EnvVarsEffectiveConfigFileFactory(ConfigProperties config) {
    this.config = config;
  }

  @Override
  public AgentConfigFile createFile() {
    ByteString content = new ByteString(buildFileContent().getBytes(UTF_8));
    return new AgentConfigFile(content, "text/plain; format=properties; vendor=splunk; v=1.0.0");
  }

  public String buildFileContent() {
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
        .add(SPLUNK_PROFILER_ENABLED, profilerConfiguration.isEnabled())
        .add(SPLUNK_PROFILER_MEMORY_ENABLED, profilerConfiguration.getMemoryEnabled())
        .add(SPLUNK_SNAPSHOT_PROFILER_ENABLED, snapshotConfiguration.isEnabled())
        .add(
            SPLUNK_SNAPSHOT_PROFILER_SAMPLING_INTERVAL, snapshotConfiguration.getSamplingInterval())
        .add(SPLUNK_PROFILER_CALL_STACK_INTERVAL, profilerConfiguration.getCallStackInterval());
  }

  private void addOtelEnvVars(EffectiveConfigBuilder builder) {
    builder
        .add(OTEL_EXPORTER_OTLP_TRACES_ENDPOINT, getSignalEndpoint(config, OTLP_SIGNAL_TRACES))
        .add(OTEL_EXPORTER_OTLP_METRICS_ENDPOINT, getSignalEndpoint(config, OTLP_SIGNAL_METRICS))
        .add(OTEL_EXPORTER_OTLP_LOGS_ENDPOINT, getSignalEndpoint(config, OTLP_SIGNAL_LOGS));
  }

  private static String getSignalEndpoint(ConfigProperties config, String signal) {
    // If otlp exporter is not enabled for given signal then return empty string
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
    String baseEndpoint = config.getString(OTEL_EXPORTER_OTLP_ENDPOINT);
    boolean isGrpc = OTLP_PROTOCOL_GRPC.equals(getSignalOtlpProtocol(config, signal));
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
    return config.getString(OTEL_EXPORTER_OTLP_PROTOCOL, OTLP_PROTOCOL_HTTP_PROTOBUF);
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
