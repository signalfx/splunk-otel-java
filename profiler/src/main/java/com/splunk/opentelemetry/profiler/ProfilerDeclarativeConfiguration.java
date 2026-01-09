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
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class ProfilerDeclarativeConfiguration implements ProfilerConfiguration {
  private static final Logger logger =
      Logger.getLogger(ProfilerDeclarativeConfiguration.class.getName());

  private static final String DEFAULT_PROFILER_DIRECTORY = System.getProperty("java.io.tmpdir");
  private static final long DEFAULT_RECORDING_DURATION = Duration.ofSeconds(20).toMillis();
  private static final long DEFAULT_SAMPLING_INTERVAL = Duration.ofSeconds(10).toMillis();

  private static final String MEMORY_PROFILER = "memory_profiler";
  private static final String MEMORY_EVENT_RATE = "event_rate";

  private final DeclarativeConfigProperties profilingConfig;

  public ProfilerDeclarativeConfiguration(DeclarativeConfigProperties profilingConfig) {
    this.profilingConfig = profilingConfig;
  }

  @Override
  public boolean isEnabled() {
    return !profilingConfig.equals(empty());
  }

  @Override
  public void log() {
    logger.info("-----------------------");
    logger.info("Profiler declarative configuration:");
    log("Enabled", isEnabled());
    log("ProfilerDirectory", getProfilerDirectory());
    log("RecordingDuration", getRecordingDuration().toMillis() + "ms");
    log("KeepFiles", getKeepFiles());
    log("MemoryEnabled", getMemoryEnabled());
    if (getMemoryEventRateLimitEnabled()) {
      log("MemoryEventRate", getMemoryEventRate());
    }
    log("UseAllocationSampleEvent", getUseAllocationSampleEvent());
    log("CallStackInterval", getCallStackInterval().toMillis() + "ms");
    log("IncludeAgentInternalStacks", getIncludeAgentInternalStacks());
    log("IncludeJvmInternalStacks", getIncludeJvmInternalStacks());
    log("TracingStacksOnly", getTracingStacksOnly());
    log("StackDepth", getStackDepth());
    logger.info("-----------------------");
  }

  private static void log(String key, @Nullable Object value) {
    logger.info(String.format("%30s : %s", key, value));
  }

  @Override
  public String getIngestUrl() {
    throw new UnsupportedOperationException("Not supported for declarative configuration");
  }

  @Override
  public String getOtlpProtocol() {
    throw new UnsupportedOperationException("Not supported for declarative configuration");
  }

  @Override
  public boolean getMemoryEnabled() {
    return getConfigRoot().getStructured(MEMORY_PROFILER) != null;
  }

  @Override
  public boolean getMemoryEventRateLimitEnabled() {
    return getMemoryProfilerConfig().getString(MEMORY_EVENT_RATE) != null;
  }

  @Override
  public String getMemoryEventRate() {
    return getMemoryProfilerConfig().getString(MEMORY_EVENT_RATE, "150/s");
  }

  @Override
  public boolean getUseAllocationSampleEvent() {
    // Using jdk16+ ObjectAllocationSample event is disabled by default
    return HAS_OBJECT_ALLOCATION_SAMPLE_EVENT
        && getMemoryProfilerConfig().getBoolean("native_sampling", false);
  }

  @Override
  public Duration getCallStackInterval() {
    return getDuration(
        getConfigRoot().getStructured("cpu_profiler", empty()),
        "sampling_interval",
        DEFAULT_SAMPLING_INTERVAL);
  }

  @Override
  public boolean getIncludeAgentInternalStacks() {
    return getConfigRoot().getBoolean("include_agent_internals", false);
  }

  @Override
  public boolean getIncludeJvmInternalStacks() {
    return getConfigRoot().getBoolean("include_jvm_internals", false);
  }

  @Override
  public boolean getTracingStacksOnly() {
    return getConfigRoot().getBoolean("tracing_stacks_only", false);
  }

  @Override
  public int getStackDepth() {
    return getConfigRoot().getInt("stack_depth", 1024);
  }

  @Override
  public boolean getKeepFiles() {
    return getConfigRoot().getBoolean("keep_recording_files", false);
  }

  @Override
  public String getProfilerDirectory() {
    return getConfigRoot().getString("recording_directory", DEFAULT_PROFILER_DIRECTORY);
  }

  @Override
  public Duration getRecordingDuration() {
    return getDuration(getConfigRoot(), "recording_duration", DEFAULT_RECORDING_DURATION);
  }

  @Override
  public DeclarativeConfigProperties getConfigProperties() {
    return profilingConfig;
  }

  private DeclarativeConfigProperties getConfigRoot() {
    return profilingConfig.getStructured("always_on", empty());
  }

  private DeclarativeConfigProperties getMemoryProfilerConfig() {
    return getConfigRoot().getStructured(MEMORY_PROFILER, empty());
  }

  private static Duration getDuration(
      DeclarativeConfigProperties config, String key, long defaultValue) {
    return Duration.ofMillis(config.getLong(key, defaultValue));
  }
}
