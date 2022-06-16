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

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.config.ConfigCustomizer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@AutoService(ConfigCustomizer.class)
public class Configuration implements ConfigCustomizer {

  private static final String DEFAULT_RECORDING_DURATION = "20s";
  public static final boolean DEFAULT_MEMORY_ENABLED = false;
  public static final int DEFAULT_MEMORY_SAMPLING_INTERVAL = 1;
  public static final Duration DEFAULT_CALL_STACK_INTERVAL = Duration.ofSeconds(10);
  public static final boolean DEFAULT_INCLUDE_INTERNAL_STACKS = false;
  public static final boolean DEFAULT_TRACING_STACKS_ONLY = false;
  private static final int DEFAULT_STACK_DEPTH = 1024;

  public static final String CONFIG_KEY_ENABLE_PROFILER = PROFILER_ENABLED_PROPERTY;
  public static final String CONFIG_KEY_PROFILER_DIRECTORY = "splunk.profiler.directory";
  public static final String CONFIG_KEY_RECORDING_DURATION = "splunk.profiler.recording.duration";
  public static final String CONFIG_KEY_KEEP_FILES = "splunk.profiler.keep-files";
  public static final String CONFIG_KEY_INGEST_URL = "splunk.profiler.logs-endpoint";
  public static final String CONFIG_KEY_OTEL_OTLP_URL = "otel.exporter.otlp.endpoint";
  public static final String CONFIG_KEY_MEMORY_ENABLED = PROFILER_MEMORY_ENABLED_PROPERTY;
  public static final String CONFIG_KEY_MEMORY_SAMPLER_INTERVAL =
      "splunk.profiler.memory.sampler.interval";
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
  public Map<String, String> defaultProperties() {
    HashMap<String, String> config = new HashMap<>();
    config.put(CONFIG_KEY_ENABLE_PROFILER, "false");
    config.put(CONFIG_KEY_PROFILER_DIRECTORY, ".");
    config.put(CONFIG_KEY_RECORDING_DURATION, DEFAULT_RECORDING_DURATION);
    config.put(CONFIG_KEY_KEEP_FILES, "false");
    config.put(CONFIG_KEY_MEMORY_ENABLED, String.valueOf(DEFAULT_MEMORY_ENABLED));
    config.put(
        CONFIG_KEY_MEMORY_SAMPLER_INTERVAL, String.valueOf(DEFAULT_MEMORY_SAMPLING_INTERVAL));
    config.put(CONFIG_KEY_CALL_STACK_INTERVAL, DEFAULT_CALL_STACK_INTERVAL.toMillis() + "ms");
    return config;
  }

  public static String getConfigUrl(Config config) {
    String ingestUrl = config.getString(CONFIG_KEY_OTEL_OTLP_URL, null);
    return config.getString(CONFIG_KEY_INGEST_URL, ingestUrl);
  }

  public static boolean getTLABEnabled(Config config) {
    boolean memoryEnabled = config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, DEFAULT_MEMORY_ENABLED);
    return config.getBoolean(CONFIG_KEY_TLAB_ENABLED, memoryEnabled);
  }

  public static int getMemorySamplerInterval(Config config) {
    return config.getInt(CONFIG_KEY_MEMORY_SAMPLER_INTERVAL, DEFAULT_MEMORY_SAMPLING_INTERVAL);
  }

  public static Duration getCallStackInterval(Config config) {
    return config.getDuration(CONFIG_KEY_CALL_STACK_INTERVAL, DEFAULT_CALL_STACK_INTERVAL);
  }

  public static boolean getIncludeAgentInternalStacks(Config config) {
    boolean includeInternals =
        config.getBoolean(CONFIG_KEY_INCLUDE_INTERNAL_STACKS, DEFAULT_INCLUDE_INTERNAL_STACKS);
    return config.getBoolean(CONFIG_KEY_INCLUDE_AGENT_INTERNALS, includeInternals);
  }

  public static boolean getIncludeJvmInternalStacks(Config config) {
    boolean includeInternals =
        config.getBoolean(CONFIG_KEY_INCLUDE_INTERNAL_STACKS, DEFAULT_INCLUDE_INTERNAL_STACKS);
    return config.getBoolean(CONFIG_KEY_INCLUDE_JVM_INTERNALS, includeInternals);
  }

  public static boolean getTracingStacksOnly(Config config) {
    return config.getBoolean(CONFIG_KEY_TRACING_STACKS_ONLY, DEFAULT_TRACING_STACKS_ONLY);
  }

  public static int getStackDepth(Config config) {
    return config.getInt(CONFIG_KEY_STACK_DEPTH, DEFAULT_STACK_DEPTH);
  }

  public static DataFormat getCpuDataFormat(Config config) {
    String value =
        config.getString(CONFIG_KEY_CPU_DATA_FORMAT, DataFormat.PPROF_GZIP_BASE64.value());
    return DataFormat.fromString(value);
  }

  public static DataFormat getAllocationDataFormat(Config config) {
    String value =
        config.getString(CONFIG_KEY_MEMORY_DATA_FORMAT, DataFormat.PPROF_GZIP_BASE64.value());
    return DataFormat.fromString(value);
  }

  public enum DataFormat {
    TEXT,
    PPROF_GZIP_BASE64;

    private final String value;

    DataFormat() {
      value = name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static DataFormat fromString(String value) {
      return DataFormat.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'));
    }

    public String value() {
      return value;
    }
  }
}
