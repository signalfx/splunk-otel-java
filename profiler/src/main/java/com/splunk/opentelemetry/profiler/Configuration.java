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
import static com.splunk.opentelemetry.SplunkConfiguration.PROFILER_MEMORY_ENABLED_PROPERTY;
import static java.util.logging.Level.WARNING;

import com.splunk.opentelemetry.SplunkConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.function.Function;
import java.util.logging.Logger;

public class Configuration {
  private static final Logger logger = Logger.getLogger(Configuration.class.getName());

  /* Keys visible for testing */
  static final String CONFIG_KEY_PROFILER_DIRECTORY = "splunk.profiler.directory";
  static final String CONFIG_KEY_RECORDING_DURATION = "splunk.profiler.recording.duration";
  static final String CONFIG_KEY_KEEP_FILES = "splunk.profiler.keep-files";
  static final String CONFIG_KEY_INGEST_URL = "splunk.profiler.logs-endpoint";
  static final String CONFIG_KEY_PROFILER_OTLP_PROTOCOL = "splunk.profiler.otlp.protocol";
  static final String CONFIG_KEY_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
  static final String CONFIG_KEY_OTEL_OTLP_URL = "otel.exporter.otlp.endpoint";
  static final String CONFIG_KEY_MEMORY_ENABLED = PROFILER_MEMORY_ENABLED_PROPERTY;
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

  private static final Duration DEFAULT_RECORDING_DURATION = Duration.ofSeconds(20);
  private static final boolean DEFAULT_MEMORY_ENABLED = false;
  private static final Duration DEFAULT_CALL_STACK_INTERVAL = Duration.ofSeconds(10);
  private static final boolean DEFAULT_INCLUDE_INTERNAL_STACKS = false;
  private static final boolean DEFAULT_TRACING_STACKS_ONLY = false;
  private static final int DEFAULT_STACK_DEPTH = 1024;
  private static final boolean DEFAULT_MEMORY_EVENT_RATE_LIMIT_ENABLED = true;
  // ObjectAllocationSample event uses 150/s in default and 300/s in profiling configuration
  private static final String DEFAULT_MEMORY_EVENT_RATE = "150/s";

  public static void log(ConfigProperties config) {
    logger.info("-----------------------");
    logger.info("Profiler configuration:");
    log(PROFILER_ENABLED_PROPERTY, (it) -> SplunkConfiguration.isProfilerEnabled(config));
    log(CONFIG_KEY_PROFILER_DIRECTORY, (it) -> getProfilerDirectory(config));
    log(CONFIG_KEY_RECORDING_DURATION, (it) -> getRecordingDuration(config));
    log(CONFIG_KEY_KEEP_FILES, (it) -> getKeepFiles(config));
    log(CONFIG_KEY_INGEST_URL, (it) -> getConfigUrl(config));
    log(CONFIG_KEY_OTEL_OTLP_URL, config::getString);
    log(CONFIG_KEY_MEMORY_ENABLED, (it) -> getMemoryEnabled(config));
    if (getMemoryEventRateLimitEnabled(config)) {
      log(CONFIG_KEY_MEMORY_EVENT_RATE, (it) -> getMemoryEventRate(config));
    }
    log(CONFIG_KEY_CALL_STACK_INTERVAL, (it) -> getCallStackInterval(config));
    log(CONFIG_KEY_INCLUDE_AGENT_INTERNALS, (it) -> getIncludeAgentInternalStacks(config));
    log(CONFIG_KEY_INCLUDE_JVM_INTERNALS, (it) -> getIncludeJvmInternalStacks(config));
    log(CONFIG_KEY_TRACING_STACKS_ONLY, (it) -> getTracingStacksOnly(config));
    log(CONFIG_KEY_STACK_DEPTH, (it) -> getStackDepth(config));
    logger.info("-----------------------");
  }

  private static void log(String key, Function<String, Object> getter) {
    logger.info(String.format("%39s : %s", key, getter.apply(key)));
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
    return config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, DEFAULT_MEMORY_ENABLED);
  }

  public static boolean getMemoryEventRateLimitEnabled(ConfigProperties config) {
    return config.getBoolean(
        CONFIG_KEY_MEMORY_EVENT_RATE_LIMIT_ENABLED, DEFAULT_MEMORY_EVENT_RATE_LIMIT_ENABLED);
  }

  public static String getMemoryEventRate(ConfigProperties config) {
    return config.getString(CONFIG_KEY_MEMORY_EVENT_RATE, DEFAULT_MEMORY_EVENT_RATE);
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
    boolean includeInternals =
        config.getBoolean(CONFIG_KEY_INCLUDE_INTERNAL_STACKS, DEFAULT_INCLUDE_INTERNAL_STACKS);
    return config.getBoolean(CONFIG_KEY_INCLUDE_AGENT_INTERNALS, includeInternals);
  }

  public static boolean getIncludeJvmInternalStacks(ConfigProperties config) {
    boolean includeInternals =
        config.getBoolean(CONFIG_KEY_INCLUDE_INTERNAL_STACKS, DEFAULT_INCLUDE_INTERNAL_STACKS);
    return config.getBoolean(CONFIG_KEY_INCLUDE_JVM_INTERNALS, includeInternals);
  }

  public static boolean getTracingStacksOnly(ConfigProperties config) {
    return config.getBoolean(CONFIG_KEY_TRACING_STACKS_ONLY, DEFAULT_TRACING_STACKS_ONLY);
  }

  public static int getStackDepth(ConfigProperties config) {
    return config.getInt(CONFIG_KEY_STACK_DEPTH, DEFAULT_STACK_DEPTH);
  }

  public static boolean getKeepFiles(ConfigProperties config) {
    return config.getBoolean(CONFIG_KEY_KEEP_FILES, false);
  }

  public static String getProfilerDirectory(ConfigProperties config) {
    return config.getString(CONFIG_KEY_PROFILER_DIRECTORY, System.getProperty("java.io.tmpdir"));
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
