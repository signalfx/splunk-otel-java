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
  private static final String OTEL_EXPORTER_OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";
  private static final String OTEL_EXPORTER_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
  private static final String OTLP_PROTOCOL_HTTP_PROTOBUF = "http/protobuf";
  private static final String OTLP_SIGNAL_LOGS = "logs";
  private static final String OTLP_SIGNAL_METRICS = "metrics";
  private static final String OTLP_SIGNAL_TRACES = "traces";

  private final ConfigProperties config;

  EnvVarsEffectiveConfigFileFactory(ConfigProperties config) {
    this.config = config;
  }

  @Override
  public String buildFileContent() {
    return addOtelEnvVars(addSplunkEnvVars(new EffectiveConfigBuilder())).build();
  }

  private EffectiveConfigBuilder addSplunkEnvVars(EffectiveConfigBuilder builder) {
    ProfilerConfiguration profilerConfiguration = new ProfilerEnvVarsConfiguration(config);
    SnapshotProfilingConfiguration snapshotConfiguration =
        new SnapshotProfilingEnvVarsConfiguration(config);

    return addSplunkEnvVars(builder, profilerConfiguration, snapshotConfiguration);
  }

  private EffectiveConfigBuilder addOtelEnvVars(EffectiveConfigBuilder builder) {
    return builder
        .add(OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS, getSignalEndpoint(config, OTLP_SIGNAL_TRACES))
        .add(OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS, getSignalEndpoint(config, OTLP_SIGNAL_METRICS))
        .add(OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS, getSignalEndpoint(config, OTLP_SIGNAL_LOGS));
  }

  private static String getSignalEndpoint(ConfigProperties config, String signal) {
    String propertyName = "otel.exporter.otlp." + signal + ".endpoint";
    String endpoint = config.getString(propertyName);
    if (endpoint != null) {
      return endpoint;
    }

    String baseEndpoint = config.getString(OTEL_EXPORTER_OTLP_ENDPOINT);
    boolean isProtBuf = OTLP_PROTOCOL_HTTP_PROTOBUF.equals(getSignalOtlpProtocol(config, signal));
    if (baseEndpoint == null) {
      return isProtBuf ? "http://localhost:4318/v1/" + signal : "http://localhost:4317";
    }
    if (isProtBuf) {
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
