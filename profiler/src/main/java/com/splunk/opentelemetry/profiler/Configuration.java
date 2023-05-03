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

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class Configuration implements AutoConfigurationCustomizerProvider {
  private static final Logger logger = Logger.getLogger(Configuration.class.getName());
  private static final boolean HAS_OBJECT_ALLOCATION_SAMPLE_EVENT = getJavaVersion() >= 16;

  private static final String DEFAULT_RECORDING_DURATION = "20s";
  public static final boolean DEFAULT_MEMORY_ENABLED = false;
  public static final Duration DEFAULT_CALL_STACK_INTERVAL = Duration.ofSeconds(10);
  public static final boolean DEFAULT_INCLUDE_INTERNAL_STACKS = false;
  public static final boolean DEFAULT_TRACING_STACKS_ONLY = false;
  private static final int DEFAULT_STACK_DEPTH = 1024;
  private static final boolean DEFAULT_MEMORY_EVENT_RATE_LIMIT_ENABLED = true;

  public static final String CONFIG_KEY_ENABLE_PROFILER = PROFILER_ENABLED_PROPERTY;
  public static final String CONFIG_KEY_PROFILER_DIRECTORY = "splunk.profiler.directory";
  public static final String CONFIG_KEY_RECORDING_DURATION = "splunk.profiler.recording.duration";
  public static final String CONFIG_KEY_KEEP_FILES = "splunk.profiler.keep-files";
  public static final String CONFIG_KEY_INGEST_URL = "splunk.profiler.logs-endpoint";
  public static final String CONFIG_KEY_OTEL_OTLP_URL = "otel.exporter.otlp.endpoint";
  public static final String CONFIG_KEY_MEMORY_ENABLED = PROFILER_MEMORY_ENABLED_PROPERTY;
  public static final String CONFIG_KEY_MEMORY_EVENT_RATE_LIMIT_ENABLED =
      "splunk.profiler.memory.event.rate-limit.enabled";
  // ObjectAllocationSample event uses 150/s in default and 300/s in profiling configuration
  private static final String DEFAULT_MEMORY_EVENT_RATE = "150/s";
  public static final String CONFIG_KEY_MEMORY_EVENT_RATE = "splunk.profiler.memory.event.rate";
  public static final String CONFIG_KEY_MEMORY_NATIVE_SAMPLING =
      "splunk.profiler.memory.native.sampling";
  private static final String CONFIG_KEY_CPU_DATA_FORMAT = "splunk.profiler.cpu.data.format";
  private static final String CONFIG_KEY_MEMORY_DATA_FORMAT = "splunk.profiler.memory.data.format";
  public static final String CONFIG_KEY_TLAB_ENABLED = "splunk.profiler.tlab.enabled";
  public static final String CONFIG_KEY_CALL_STACK_INTERVAL = "splunk.profiler.call.stack.interval";
  public static final String CONFIG_KEY_INCLUDE_AGENT_INTERNALS =
      "splunk.profiler.include.agent.internals";
  // Include stacks where every frame starts with jvm/sun/jdk
  public static final String CONFIG_KEY_INCLUDE_JVM_INTERNALS =
      "splunk.profiler.include.jvm.internals";
  public static final String CONFIG_KEY_INCLUDE_INTERNAL_STACKS =
      "splunk.profiler.include.internal.stacks";
  public static final String CONFIG_KEY_TRACING_STACKS_ONLY = "splunk.profiler.tracing.stacks.only";
  private static final String CONFIG_KEY_STACK_DEPTH = "splunk.profiler.max.stack.depth";

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addPropertiesSupplier(this::defaultProperties);
  }

  Map<String, String> defaultProperties() {
    HashMap<String, String> config = new HashMap<>();
    config.put(CONFIG_KEY_ENABLE_PROFILER, "false");
    config.put(CONFIG_KEY_PROFILER_DIRECTORY, System.getProperty("java.io.tmpdir"));
    config.put(CONFIG_KEY_RECORDING_DURATION, DEFAULT_RECORDING_DURATION);
    config.put(CONFIG_KEY_KEEP_FILES, "false");
    config.put(CONFIG_KEY_MEMORY_ENABLED, String.valueOf(DEFAULT_MEMORY_ENABLED));
    config.put(CONFIG_KEY_MEMORY_EVENT_RATE, DEFAULT_MEMORY_EVENT_RATE);
    config.put(CONFIG_KEY_CALL_STACK_INTERVAL, DEFAULT_CALL_STACK_INTERVAL.toMillis() + "ms");
    return config;
  }

  public static String getConfigUrl(ConfigProperties config) {
    String ingestUrl = config.getString(CONFIG_KEY_OTEL_OTLP_URL, null);
    if (ingestUrl != null
        && ingestUrl.startsWith("https://ingest.")
        && ingestUrl.endsWith(".signalfx.com")
        && config.getString(CONFIG_KEY_INGEST_URL) == null) {
      logger.log(
          WARNING,
          "Profiling data can not be sent to {0}, using http://localhost:4317 instead. "
              + "You can override it by setting splunk.profiler.logs-endpoint",
          new Object[] {ingestUrl});
      return null;
    }
    return config.getString(CONFIG_KEY_INGEST_URL, ingestUrl);
  }

  public static boolean getTLABEnabled(ConfigProperties config) {
    boolean memoryEnabled = config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, DEFAULT_MEMORY_ENABLED);
    return config.getBoolean(CONFIG_KEY_TLAB_ENABLED, memoryEnabled);
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

  private static int getJavaVersion() {
    String javaSpecVersion = System.getProperty("java.specification.version");
    if ("1.8".equals(javaSpecVersion)) {
      return 8;
    }
    return Integer.parseInt(javaSpecVersion);
  }
}
