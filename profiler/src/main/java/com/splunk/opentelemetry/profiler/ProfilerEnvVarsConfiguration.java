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

import com.splunk.opentelemetry.SplunkConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class ProfilerEnvVarsConfiguration implements ProfilerConfiguration {
  private static final Logger logger =
      Logger.getLogger(ProfilerEnvVarsConfiguration.class.getName());

  static final String CONFIG_KEY_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
  static final String CONFIG_KEY_OTEL_OTLP_URL = "otel.exporter.otlp.endpoint";

  /* Keys visible for testing */
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

  private final ConfigProperties config;

  public ProfilerEnvVarsConfiguration(ConfigProperties config) {
    this.config = config;
  }

  @Override
  public void log() {
    logger.info("-----------------------");
    logger.info("Profiler env vars based configuration:");
    log(SplunkConfiguration.PROFILER_ENABLED_PROPERTY, isEnabled());
    log(CONFIG_KEY_PROFILER_DIRECTORY, getProfilerDirectory());
    log(CONFIG_KEY_RECORDING_DURATION, getRecordingDuration().toMillis() + "ms");
    log(CONFIG_KEY_KEEP_FILES, getKeepFiles());
    log(CONFIG_KEY_PROFILER_OTLP_PROTOCOL, getOtlpProtocol());
    log(CONFIG_KEY_INGEST_URL, getIngestUrl());
    log(CONFIG_KEY_OTEL_OTLP_URL, config.getString(CONFIG_KEY_OTEL_OTLP_URL));
    log(CONFIG_KEY_MEMORY_ENABLED, getMemoryEnabled());
    if (getMemoryEventRateLimitEnabled()) {
      log(CONFIG_KEY_MEMORY_EVENT_RATE, getMemoryEventRate());
    }
    log(CONFIG_KEY_MEMORY_NATIVE_SAMPLING, getUseAllocationSampleEvent());
    log(CONFIG_KEY_CALL_STACK_INTERVAL, getCallStackInterval().toMillis() + "ms");
    log(CONFIG_KEY_INCLUDE_AGENT_INTERNALS, getIncludeAgentInternalStacks());
    log(CONFIG_KEY_INCLUDE_JVM_INTERNALS, getIncludeJvmInternalStacks());
    log(CONFIG_KEY_TRACING_STACKS_ONLY, getTracingStacksOnly());
    log(CONFIG_KEY_STACK_DEPTH, getStackDepth());
    logger.info("-----------------------");
  }

  private void log(String key, @Nullable Object value) {
    logger.info(String.format("%39s : %s", key, value));
  }

  @Override
  public boolean isEnabled() {
    return SplunkConfiguration.isProfilerEnabled(config);
  }

  @Override
  public String getIngestUrl() {
    String ingestUrl = config.getString(CONFIG_KEY_INGEST_URL);

    if (ingestUrl == null) {
      String defaultIngestUrl = getDefaultLogsEndpoint();
      ingestUrl = config.getString(CONFIG_KEY_OTEL_OTLP_URL, defaultIngestUrl);

      if (ingestUrl.startsWith("https://ingest.") && ingestUrl.endsWith(".signalfx.com")) {
        logger.log(
            WARNING,
            "Profiling data can not be sent to {0}, using {1} instead. "
                + "You can override it by setting "
                + CONFIG_KEY_INGEST_URL,
            new Object[] {ingestUrl, defaultIngestUrl});
        return defaultIngestUrl;
      }

      if ("http/protobuf".equals(getOtlpProtocol())) {
        ingestUrl = maybeAppendHttpPath(ingestUrl);
      }
    }
    return ingestUrl;
  }

  private String maybeAppendHttpPath(String ingestUrl) {
    if (!ingestUrl.endsWith("v1/logs")) {
      if (!ingestUrl.endsWith("/")) {
        ingestUrl += "/";
      }
      ingestUrl += "v1/logs";
    }
    return ingestUrl;
  }

  private String getDefaultLogsEndpoint() {
    return "http/protobuf".equals(getOtlpProtocol())
        ? "http://localhost:4318/v1/logs"
        : "http://localhost:4317";
  }

  @Override
  public String getOtlpProtocol() {
    return config.getString(
        CONFIG_KEY_PROFILER_OTLP_PROTOCOL,
        config.getString(CONFIG_KEY_OTLP_PROTOCOL, "http/protobuf"));
  }

  @Override
  public boolean getMemoryEnabled() {
    return config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, false);
  }

  @Override
  public boolean getMemoryEventRateLimitEnabled() {
    return config.getBoolean(CONFIG_KEY_MEMORY_EVENT_RATE_LIMIT_ENABLED, true);
  }

  @Override
  public String getMemoryEventRate() {
    return config.getString(CONFIG_KEY_MEMORY_EVENT_RATE, "150/s");
  }

  @Override
  public boolean getUseAllocationSampleEvent() {
    return HAS_OBJECT_ALLOCATION_SAMPLE_EVENT
        && config.getBoolean(CONFIG_KEY_MEMORY_NATIVE_SAMPLING, false);
  }

  @Override
  public Duration getCallStackInterval() {
    return config.getDuration(CONFIG_KEY_CALL_STACK_INTERVAL, DEFAULT_CALL_STACK_INTERVAL);
  }

  @Override
  public boolean getIncludeAgentInternalStacks() {
    boolean includeInternals = config.getBoolean(CONFIG_KEY_INCLUDE_INTERNAL_STACKS, false);
    return config.getBoolean(CONFIG_KEY_INCLUDE_AGENT_INTERNALS, includeInternals);
  }

  @Override
  public boolean getIncludeJvmInternalStacks() {
    boolean includeInternals = config.getBoolean(CONFIG_KEY_INCLUDE_INTERNAL_STACKS, false);
    return config.getBoolean(CONFIG_KEY_INCLUDE_JVM_INTERNALS, includeInternals);
  }

  @Override
  public boolean getTracingStacksOnly() {
    return config.getBoolean(CONFIG_KEY_TRACING_STACKS_ONLY, false);
  }

  @Override
  public int getStackDepth() {
    return config.getInt(CONFIG_KEY_STACK_DEPTH, 1024);
  }

  @Override
  public boolean getKeepFiles() {
    return config.getBoolean(CONFIG_KEY_KEEP_FILES, false);
  }

  @Override
  public String getProfilerDirectory() {
    return config.getString(CONFIG_KEY_PROFILER_DIRECTORY, DEFAULT_PROFILER_DIRECTORY);
  }

  @Override
  public Duration getRecordingDuration() {
    return config.getDuration(CONFIG_KEY_RECORDING_DURATION, DEFAULT_RECORDING_DURATION);
  }

  @Override
  public ConfigProperties getConfigProperties() {
    return config;
  }
}
