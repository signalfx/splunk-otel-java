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

package com.splunk.opentelemetry.profiler;

import static java.util.logging.Level.WARNING;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.logging.Logger;

public final class ProfilerEnvVarsConfigurationFactory {
  private static final Logger logger =
      Logger.getLogger(ProfilerEnvVarsConfigurationFactory.class.getName());

  static final String CONFIG_KEY_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
  static final String CONFIG_KEY_OTEL_OTLP_URL = "otel.exporter.otlp.endpoint";

  /* Keys visible for testing */
  static final String CONFIG_KEY_PROFILER_ENABLED = "splunk.profiler.enabled";
  static final String CONFIG_KEY_PROFILER_DIRECTORY = "splunk.profiler.directory";
  static final String CONFIG_KEY_RECORDING_DURATION = "splunk.profiler.recording.duration";
  static final String CONFIG_KEY_KEEP_FILES = "splunk.profiler.keep-files";
  static final String CONFIG_KEY_INGEST_URL = "splunk.profiler.logs-endpoint";
  static final String CONFIG_KEY_PROFILER_OTLP_PROTOCOL = "splunk.profiler.otlp.protocol";
  static final String CONFIG_KEY_MEMORY_ENABLED = "splunk.profiler.memory.enabled";
  static final String CONFIG_KEY_MEMORY_EVENT_RATE_LIMIT_ENABLED =
      "splunk.profiler.memory.event.rate-limit.enabled";
  static final String CONFIG_KEY_MEMORY_EVENT_RATE = "splunk.profiler.memory.event.rate";
  static final String CONFIG_KEY_MEMORY_NATIVE_SAMPLING = "splunk.profiler.memory.native.sampling";
  static final String CONFIG_KEY_CALL_STACK_INTERVAL = "splunk.profiler.call.stack.interval";
  static final String CONFIG_KEY_INCLUDE_AGENT_INTERNALS =
      "splunk.profiler.include.agent.internals";
  // Include stacks where every frame starts with jvm/sun/jdk
  static final String CONFIG_KEY_INCLUDE_JVM_INTERNALS = "splunk.profiler.include.jvm.internals";
  static final String CONFIG_KEY_INCLUDE_INTERNAL_STACKS =
      "splunk.profiler.include.internal.stacks";
  static final String CONFIG_KEY_TRACING_STACKS_ONLY = "splunk.profiler.tracing.stacks.only";
  static final String CONFIG_KEY_STACK_DEPTH = "splunk.profiler.max.stack.depth";

  private static final String DEFAULT_PROFILER_DIRECTORY = System.getProperty("java.io.tmpdir");
  private static final Duration DEFAULT_RECORDING_DURATION = Duration.ofSeconds(20);
  private static final Duration DEFAULT_CALL_STACK_INTERVAL = Duration.ofSeconds(10);

  private ProfilerEnvVarsConfigurationFactory() {}

  public static ProfilerConfiguration create(ConfigProperties config) {
    String otlpProtocol = getOtlpProtocol(config);
    boolean includeInternals = config.getBoolean(CONFIG_KEY_INCLUDE_INTERNAL_STACKS, false);

    boolean useAllocationSampleEvent =
        ProfilerConfiguration.HAS_OBJECT_ALLOCATION_SAMPLE_EVENT
            && config.getBoolean(CONFIG_KEY_MEMORY_NATIVE_SAMPLING, false);
    return ProfilerConfiguration.builder()
        .setEnabled(config.getBoolean(CONFIG_KEY_PROFILER_ENABLED, false))
        .setIngestUrl(getIngestUrl(config, otlpProtocol))
        .setOtlpProtocol(otlpProtocol)
        .setMemoryEnabled(config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, false))
        .setMemoryEventRateLimitEnabled(
            config.getBoolean(CONFIG_KEY_MEMORY_EVENT_RATE_LIMIT_ENABLED, true))
        .setMemoryEventRate(config.getString(CONFIG_KEY_MEMORY_EVENT_RATE, "150/s"))
        .setUseAllocationSampleEvent(useAllocationSampleEvent)
        .setCallStackInterval(
            config.getDuration(CONFIG_KEY_CALL_STACK_INTERVAL, DEFAULT_CALL_STACK_INTERVAL))
        .setIncludeAgentInternalStacks(
            config.getBoolean(CONFIG_KEY_INCLUDE_AGENT_INTERNALS, includeInternals))
        .setIncludeJvmInternalStacks(
            config.getBoolean(CONFIG_KEY_INCLUDE_JVM_INTERNALS, includeInternals))
        .setTracingStacksOnly(config.getBoolean(CONFIG_KEY_TRACING_STACKS_ONLY, false))
        .setStackDepth(config.getInt(CONFIG_KEY_STACK_DEPTH, 1024))
        .setKeepFiles(config.getBoolean(CONFIG_KEY_KEEP_FILES, false))
        .setProfilerDirectory(
            config.getString(CONFIG_KEY_PROFILER_DIRECTORY, DEFAULT_PROFILER_DIRECTORY))
        .setRecordingDuration(
            config.getDuration(CONFIG_KEY_RECORDING_DURATION, DEFAULT_RECORDING_DURATION))
        .setConfigProperties(config)
        .build();
  }

  private static String getIngestUrl(ConfigProperties config, String otlpProtocol) {
    String ingestUrl = config.getString(CONFIG_KEY_INGEST_URL);
    if (ingestUrl == null) {
      String defaultIngestUrl = getDefaultLogsEndpoint(otlpProtocol);
      ingestUrl = config.getString(CONFIG_KEY_OTEL_OTLP_URL, defaultIngestUrl);

      if (ingestUrl.startsWith("https://ingest.")
          && ingestUrl.endsWith(".observability.splunkcloud.com")) {
        logger.log(
            WARNING,
            "Profiling data can not be sent to {0}, using {1} instead. "
                + "You can override it by setting "
                + CONFIG_KEY_INGEST_URL,
            new Object[] {ingestUrl, defaultIngestUrl});
        return defaultIngestUrl;
      }

      if ("http/protobuf".equals(otlpProtocol)) {
        ingestUrl = maybeAppendHttpPath(ingestUrl);
      }
    }
    return ingestUrl;
  }

  private static String maybeAppendHttpPath(String ingestUrl) {
    if (!ingestUrl.endsWith("v1/logs")) {
      if (!ingestUrl.endsWith("/")) {
        ingestUrl += "/";
      }
      ingestUrl += "v1/logs";
    }
    return ingestUrl;
  }

  private static String getDefaultLogsEndpoint(String otlpProtocol) {
    return "http/protobuf".equals(otlpProtocol)
        ? "http://localhost:4318/v1/logs"
        : "http://localhost:4317";
  }

  private static String getOtlpProtocol(ConfigProperties config) {
    return config.getString(
        CONFIG_KEY_PROFILER_OTLP_PROTOCOL,
        config.getString(CONFIG_KEY_OTLP_PROTOCOL, "http/protobuf"));
  }
}
