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

package com.splunk.opentelemetry.opamp.effectiveconfig;

import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/** Holds a snapshot of the effective configuration reported to the backend. */
class EffectiveConfig {
  private final ProfilerConfiguration profilerEffectiveConfiguration;
  private final SnapshotProfilingConfiguration snapshotProfilingEffectiveConfiguration;
  @Nullable private final String otelConfigFile;
  @Nullable private final String otelExperimentalConfigFile;
  private final List<ExporterConfiguration> tracesExporters;
  private final List<ExporterConfiguration> metricsExporters;
  private final List<ExporterConfiguration> logsExporters;

  public EffectiveConfig(
      @Nullable String otelConfigFile,
      @Nullable String otelExperimentalConfigFile,
      List<ExporterConfiguration> tracesExporters,
      List<ExporterConfiguration> metricsExporters,
      List<ExporterConfiguration> logsExporters,
      ProfilerConfiguration profilerConfiguration,
      SnapshotProfilingConfiguration snapshotProfilingConfiguration
      ) {
    this.otelConfigFile = otelConfigFile;
    this.otelExperimentalConfigFile = otelExperimentalConfigFile;
    this.tracesExporters = Collections.unmodifiableList(new ArrayList<>(tracesExporters));
    this.metricsExporters = Collections.unmodifiableList(new ArrayList<>(metricsExporters));
    this.logsExporters = Collections.unmodifiableList(new ArrayList<>(logsExporters));
    this.profilerEffectiveConfiguration = profilerConfiguration;
    this.snapshotProfilingEffectiveConfiguration = snapshotProfilingConfiguration;
  }

  public ProfilerConfiguration getProfilerEffectiveConfiguration() {
    return profilerEffectiveConfiguration;
  }

  public SnapshotProfilingConfiguration getSnapshotProfilingEffectiveConfiguration() {
    return snapshotProfilingEffectiveConfiguration;
  }

  @Nullable
  public String getOtelConfigFile() {
    return otelConfigFile;
  }

  @Nullable
  public String getOtelExperimentalConfigFile() {
    return otelExperimentalConfigFile;
  }

  public List<ExporterConfiguration> getTracesExporters() {
    return tracesExporters;
  }

  public List<ExporterConfiguration> getMetricsExporters() {
    return metricsExporters;
  }

  public List<ExporterConfiguration> getLogsExporters() {
    return logsExporters;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof EffectiveConfig)) {
      return false;
    }
    EffectiveConfig that = (EffectiveConfig) other;
    return profilerEffectiveConfiguration.equals(that.profilerEffectiveConfiguration)
        && snapshotProfilingEffectiveConfiguration.equals(that.snapshotProfilingEffectiveConfiguration)
        && Objects.equals(otelConfigFile, that.otelConfigFile)
        && Objects.equals(otelExperimentalConfigFile, that.otelExperimentalConfigFile)
        && tracesExporters.equals(that.tracesExporters)
        && metricsExporters.equals(that.metricsExporters)
        && logsExporters.equals(that.logsExporters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        profilerEffectiveConfiguration,
        snapshotProfilingEffectiveConfiguration,
        otelConfigFile,
        otelExperimentalConfigFile,
        tracesExporters,
        metricsExporters,
        logsExporters);
  }

  /** Describes an OTLP exporter and the processor or reader that uses it. */
  public static final class ExporterConfiguration {
    private final String type;
    private final String protocol;
    private final String endpoint;

    /**
     * @param type processor or reader type, such as {@code batch}, {@code simple}, or {@code periodic}
     * @param protocol OTLP protocol, such as {@code grpc} or {@code http/protobuf}
     * @param endpoint resolved exporter endpoint
     */
    public ExporterConfiguration(String type, String protocol, String endpoint) {
      this.type = Objects.requireNonNull(type);
      this.protocol = Objects.requireNonNull(protocol);
      this.endpoint = Objects.requireNonNull(endpoint);
    }

    public String getType() {
      return type;
    }

    public String getProtocol() {
      return protocol;
    }

    public String getEndpoint() {
      return endpoint;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof ExporterConfiguration)) {
        return false;
      }
      ExporterConfiguration that = (ExporterConfiguration) other;
      return type.equals(that.type)
          && protocol.equals(that.protocol)
          && endpoint.equals(that.endpoint);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, protocol, endpoint);
    }
  }
}
