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
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
import okio.ByteString;
import opamp.proto.AgentConfigFile;

interface EffectiveConfigFactory {
  String SPLUNK_PROFILER_ENABLED = "SPLUNK_PROFILER_ENABLED";
  String SPLUNK_PROFILER_MEMORY_ENABLED = "SPLUNK_PROFILER_MEMORY_ENABLED";
  String SPLUNK_PROFILER_CALL_STACK_INTERVAL = "SPLUNK_PROFILER_CALL_STACK_INTERVAL";
  String SPLUNK_SNAPSHOT_PROFILER_ENABLED = "SPLUNK_SNAPSHOT_PROFILER_ENABLED";
  String SPLUNK_SNAPSHOT_PROFILER_SAMPLING_INTERVAL = "SPLUNK_SNAPSHOT_PROFILER_SAMPLING_INTERVAL";
  String OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS = "OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS";
  String OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS = "OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS";
  String OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS = "OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS";

  default AgentConfigFile createFile() {
    ByteString content = new ByteString(buildFileContent().getBytes(UTF_8));
    return new AgentConfigFile(content, "text/plain+properties");
  }

  default EffectiveConfigBuilder addSplunkEnvVars(
      EffectiveConfigBuilder builder,
      ProfilerConfiguration profilerConfiguration,
      SnapshotProfilingConfiguration snapshotConfiguration) {
    return builder
        .add(SPLUNK_PROFILER_ENABLED, profilerConfiguration.isEnabled())
        .add(SPLUNK_PROFILER_MEMORY_ENABLED, profilerConfiguration.getMemoryEnabled())
        .add(SPLUNK_SNAPSHOT_PROFILER_ENABLED, snapshotConfiguration.isEnabled())
        .add(
            SPLUNK_SNAPSHOT_PROFILER_SAMPLING_INTERVAL, snapshotConfiguration.getSamplingInterval())
        .add(SPLUNK_PROFILER_CALL_STACK_INTERVAL, profilerConfiguration.getCallStackInterval());
  }

  String buildFileContent();
}
