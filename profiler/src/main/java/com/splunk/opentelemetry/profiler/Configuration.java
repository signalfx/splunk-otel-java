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

import static com.splunk.opentelemetry.SplunkConfiguration.PROFILER_ENABLED_PROPERTY;
import static java.util.logging.Level.WARNING;

import com.splunk.opentelemetry.SplunkConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class Configuration {
  private static final Logger logger = Logger.getLogger(Configuration.class.getName());

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

  private static final boolean HAS_OBJECT_ALLOCATION_SAMPLE_EVENT = getJavaVersion() >= 16;

  private static final String DEFAULT_PROFILER_DIRECTORY = System.getProperty("java.io.tmpdir");
  private static final Duration DEFAULT_RECORDING_DURATION = Duration.ofSeconds(20);
  private static final Duration DEFAULT_CALL_STACK_INTERVAL = Duration.ofSeconds(10);

  public static void log(ConfigProperties config) {
    logger.info("-----------------------");
    logger.info("Profiler configuration:");
    log(PROFILER_ENABLED_PROPERTY, SplunkConfiguration.isProfilerEnabled(config));
    log(CONFIG_KEY_PROFILER_DIRECTORY, getProfilerDirectory(config));
    log(CONFIG_KEY_RECORDING_DURATION, getRecordingDuration(config).toMillis() + "ms");
    log(CONFIG_KEY_KEEP_FILES, getKeepFiles(config));
    log(CONFIG_KEY_PROFILER_OTLP_PROTOCOL, getOtlpProtocol(config));
    log(CONFIG_KEY_INGEST_URL, getConfigUrl(config));
    log(CONFIG_KEY_OTEL_OTLP_URL, config.getString(CONFIG_KEY_OTEL_OTLP_URL));
    log(CONFIG_KEY_MEMORY_ENABLED, getMemoryEnabled(config));
    if (getMemoryEventRateLimitEnabled(config)) {
      log(CONFIG_KEY_MEMORY_EVENT_RATE, getMemoryEventRate(config));
    }
    log(CONFIG_KEY_MEMORY_NATIVE_SAMPLING, getUseAllocationSampleEvent(config));
    log(CONFIG_KEY_CALL_STACK_INTERVAL, getCallStackInterval(config).toMillis() + "ms");
    log(CONFIG_KEY_INCLUDE_AGENT_INTERNALS, getIncludeAgentInternalStacks(config));
    log(CONFIG_KEY_INCLUDE_JVM_INTERNALS, getIncludeJvmInternalStacks(config));
    log(CONFIG_KEY_TRACING_STACKS_ONLY, getTracingStacksOnly(config));
    log(CONFIG_KEY_STACK_DEPTH, getStackDepth(config));
    logger.info("-----------------------");
  }

  private static void log(String key, @Nullable Object value) {
    logger.info(String.format("%39s : %s", key, value));
  }

  public static String getConfigUrl(ConfigProperties config) {
    String ingestUrl = config.getString(CONFIG_KEY_OTEL_OTLP_URL);
    if (ingestUrl != null) {
      if (ingestUrl.startsWith("https://ingest.")
          && ingestUrl.endsWith(".signalfx.com")
          && config.getString(CONFIG_KEY_INGEST_URL) == null) {
        logger.log(
            WARNING,
            "Profiling data can not be sent to {0}, using {1} instead. "
                + "You can override it by setting splunk.profiler.logs-endpoint",
            new Object[] {ingestUrl, getDefaultLogsEndpoint(config)});
        return null;
      }
      if ("http/protobuf".equals(getOtlpProtocol(config))) {
        if (!ingestUrl.endsWith("/")) {
          ingestUrl += "/";
        }
        ingestUrl += "v1/logs";
      }
    }
    return config.getString(CONFIG_KEY_INGEST_URL, ingestUrl);
  }

  private static String getDefaultLogsEndpoint(ConfigProperties config) {
    return "http/protobuf".equals(getOtlpProtocol(config))
        ? "http://localhost:4318/v1/logs"
        : "http://localhost:4317";
  }

  public static String getOtlpProtocol(ConfigProperties config) {
    return config.getString(
        CONFIG_KEY_PROFILER_OTLP_PROTOCOL,
        config.getString(CONFIG_KEY_OTLP_PROTOCOL, "http/protobuf"));
  }

  public static boolean getMemoryEnabled(ConfigProperties config) {
    return config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, false);
  }

  public static boolean getMemoryEventRateLimitEnabled(ConfigProperties config) {
    return config.getBoolean(CONFIG_KEY_MEMORY_EVENT_RATE_LIMIT_ENABLED, true);
  }

  public static String getMemoryEventRate(ConfigProperties config) {
    return config.getString(CONFIG_KEY_MEMORY_EVENT_RATE, "150/s");
  }

  public static boolean getUseAllocationSampleEvent(ConfigProperties config) {
    // Using jdk16+ ObjectAllocationSample event is disabled by default
    return HAS_OBJECT_ALLOCATION_SAMPLE_EVENT
        && config.getBoolean(CONFIG_KEY_MEMORY_NATIVE_SAMPLING, false);
  }

  public static Duration getCallStackInterval(ConfigProperties config) {
    return config.getDuration(CONFIG_KEY_CALL_STACK_INTERVAL, DEFAULT_CALL_STACK_INTERVAL);
  }

  public static boolean getIncludeAgentInternalStacks(ConfigProperties config) {
    boolean includeInternals = config.getBoolean(CONFIG_KEY_INCLUDE_INTERNAL_STACKS, false);
    return config.getBoolean(CONFIG_KEY_INCLUDE_AGENT_INTERNALS, includeInternals);
  }

  public static boolean getIncludeJvmInternalStacks(ConfigProperties config) {
    boolean includeInternals = config.getBoolean(CONFIG_KEY_INCLUDE_INTERNAL_STACKS, false);
    return config.getBoolean(CONFIG_KEY_INCLUDE_JVM_INTERNALS, includeInternals);
  }

  public static boolean getTracingStacksOnly(ConfigProperties config) {
    return config.getBoolean(CONFIG_KEY_TRACING_STACKS_ONLY, false);
  }

  public static int getStackDepth(ConfigProperties config) {
    return config.getInt(CONFIG_KEY_STACK_DEPTH, 1024);
  }

  public static boolean getKeepFiles(ConfigProperties config) {
    return config.getBoolean(CONFIG_KEY_KEEP_FILES, false);
  }

  public static String getProfilerDirectory(ConfigProperties config) {
    return config.getString(CONFIG_KEY_PROFILER_DIRECTORY, DEFAULT_PROFILER_DIRECTORY);
  }

  public static Duration getRecordingDuration(ConfigProperties config) {
    return config.getDuration(CONFIG_KEY_RECORDING_DURATION, DEFAULT_RECORDING_DURATION);
  }

  private static int getJavaVersion() {
    String javaSpecVersion = System.getProperty("java.specification.version");
    if ("1.8".equals(javaSpecVersion)) {
      return 8;
    }
    return Integer.parseInt(javaSpecVersion);
  }
}
