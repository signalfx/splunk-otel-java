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

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.time.Duration;

public final class ProfilerDeclarativeConfigurationFactory {
  private static final String ROOT_NODE_NAME = "always_on";

  private static final String DEFAULT_PROFILER_DIRECTORY = System.getProperty("java.io.tmpdir");
  private static final long DEFAULT_RECORDING_DURATION = Duration.ofSeconds(20).toMillis();
  private static final long DEFAULT_SAMPLING_INTERVAL = Duration.ofSeconds(10).toMillis();

  private static final String MEMORY_PROFILER = "memory_profiler";
  private static final String MEMORY_EVENT_RATE = "event_rate";

  private ProfilerDeclarativeConfigurationFactory() {}

  public static ProfilerConfiguration create(DeclarativeConfigProperties profilingConfig) {
    DeclarativeConfigProperties config = profilingConfig == null ? empty() : profilingConfig;
    DeclarativeConfigProperties configRoot = getConfigRoot(config);
    DeclarativeConfigProperties memoryProfilerConfig = getMemoryProfilerConfig(configRoot);

    boolean useAllocationSampleEvent =
        ProfilerConfiguration.HAS_OBJECT_ALLOCATION_SAMPLE_EVENT
            && memoryProfilerConfig.getBoolean("native_sampling", false);
    Duration callStackInterval =
        getDuration(
            configRoot.getStructured("cpu_profiler", empty()),
            "sampling_interval",
            DEFAULT_SAMPLING_INTERVAL);

    return ProfilerConfiguration.builder()
        .setEnabled(config.getPropertyKeys().contains(ROOT_NODE_NAME))
        .setMemoryEnabled(configRoot.getPropertyKeys().contains(MEMORY_PROFILER))
        .setMemoryEventRateLimitEnabled(memoryProfilerConfig.getString(MEMORY_EVENT_RATE) != null)
        .setMemoryEventRate(memoryProfilerConfig.getString(MEMORY_EVENT_RATE, "150/s"))
        .setUseAllocationSampleEvent(useAllocationSampleEvent)
        .setCallStackInterval(callStackInterval)
        .setIncludeAgentInternalStacks(configRoot.getBoolean("include_agent_internals", false))
        .setIncludeJvmInternalStacks(configRoot.getBoolean("include_jvm_internals", false))
        .setTracingStacksOnly(configRoot.getBoolean("tracing_stacks_only", false))
        .setStackDepth(configRoot.getInt("stack_depth", 1024))
        .setKeepFiles(configRoot.getBoolean("keep_recording_files", false))
        .setProfilerDirectory(
            configRoot.getString("recording_directory", DEFAULT_PROFILER_DIRECTORY))
        .setRecordingDuration(
            getDuration(configRoot, "recording_duration", DEFAULT_RECORDING_DURATION))
        .setConfigProperties(config)
        .build();
  }

  private static DeclarativeConfigProperties getConfigRoot(DeclarativeConfigProperties config) {
    return config.getStructured(ROOT_NODE_NAME, empty());
  }

  private static DeclarativeConfigProperties getMemoryProfilerConfig(
      DeclarativeConfigProperties configRoot) {
    return configRoot.getStructured(MEMORY_PROFILER, empty());
  }

  private static Duration getDuration(
      DeclarativeConfigProperties config, String key, long defaultValue) {
    return Duration.ofMillis(config.getLong(key, defaultValue));
  }
}
