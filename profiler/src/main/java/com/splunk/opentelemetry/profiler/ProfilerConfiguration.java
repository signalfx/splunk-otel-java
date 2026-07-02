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

import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import java.time.Duration;
import java.util.Objects;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class ProfilerConfiguration {
  public static final OptionalConfigurableSupplier<ProfilerConfiguration> SUPPLIER =
      new OptionalConfigurableSupplier<>();

  public static final boolean HAS_OBJECT_ALLOCATION_SAMPLE_EVENT = getJavaVersion() >= 16;

  private static final Logger logger = Logger.getLogger(ProfilerConfiguration.class.getName());
  private static final String DEFAULT_PROFILER_DIRECTORY = System.getProperty("java.io.tmpdir");
  private static final Duration DEFAULT_RECORDING_DURATION = Duration.ofSeconds(20);
  private static final Duration DEFAULT_CALL_STACK_INTERVAL = Duration.ofSeconds(10);

  private final boolean enabled;
  @Nullable private final String ingestUrl;
  @Nullable private final String otlpProtocol;
  private final boolean memoryEnabled;
  private final boolean memoryEventRateLimitEnabled;
  private final String memoryEventRate;
  private final boolean useAllocationSampleEvent;
  private final Duration callStackInterval;
  private final boolean includeAgentInternalStacks;
  private final boolean includeJvmInternalStacks;
  private final boolean tracingStacksOnly;
  private final int stackDepth;
  private final boolean keepFiles;
  private final String profilerDirectory;
  private final Duration recordingDuration;
  @Nullable private final Object configProperties;

  private ProfilerConfiguration(Builder builder) {
    enabled = builder.enabled;
    ingestUrl = builder.ingestUrl;
    otlpProtocol = builder.otlpProtocol;
    memoryEnabled = builder.memoryEnabled;
    memoryEventRateLimitEnabled = builder.memoryEventRateLimitEnabled;
    memoryEventRate = builder.memoryEventRate;
    useAllocationSampleEvent = builder.useAllocationSampleEvent;
    callStackInterval = builder.callStackInterval;
    includeAgentInternalStacks = builder.includeAgentInternalStacks;
    includeJvmInternalStacks = builder.includeJvmInternalStacks;
    tracingStacksOnly = builder.tracingStacksOnly;
    stackDepth = builder.stackDepth;
    keepFiles = builder.keepFiles;
    profilerDirectory = builder.profilerDirectory;
    recordingDuration = builder.recordingDuration;
    configProperties = builder.configProperties;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void log() {
    logger.info("-----------------------");
    logger.info("Profiler configuration:");
    log("Enabled", isEnabled());
    log("ProfilerDirectory", getProfilerDirectory());
    log("RecordingDuration", getRecordingDuration().toMillis() + "ms");
    log("KeepFiles", getKeepFiles());
    log("OtlpProtocol", getOtlpProtocol());
    log("IngestUrl", getIngestUrl());
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

  @Nullable
  public String getIngestUrl() {
    return ingestUrl;
  }

  @Nullable
  public String getOtlpProtocol() {
    return otlpProtocol;
  }

  public boolean getMemoryEnabled() {
    return memoryEnabled;
  }

  public boolean getMemoryEventRateLimitEnabled() {
    return memoryEventRateLimitEnabled;
  }

  public String getMemoryEventRate() {
    return memoryEventRate;
  }

  public boolean getUseAllocationSampleEvent() {
    return useAllocationSampleEvent;
  }

  public Duration getCallStackInterval() {
    return callStackInterval;
  }

  public boolean getIncludeAgentInternalStacks() {
    return includeAgentInternalStacks;
  }

  public boolean getIncludeJvmInternalStacks() {
    return includeJvmInternalStacks;
  }

  public boolean getTracingStacksOnly() {
    return tracingStacksOnly;
  }

  public int getStackDepth() {
    return stackDepth;
  }

  public boolean getKeepFiles() {
    return keepFiles;
  }

  public String getProfilerDirectory() {
    return profilerDirectory;
  }

  public Duration getRecordingDuration() {
    return recordingDuration;
  }

  @Nullable
  public Object getConfigProperties() {
    return configProperties;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ProfilerConfiguration)) {
      return false;
    }
    ProfilerConfiguration that = (ProfilerConfiguration) other;
    return enabled == that.enabled
        && memoryEnabled == that.memoryEnabled
        && memoryEventRateLimitEnabled == that.memoryEventRateLimitEnabled
        && useAllocationSampleEvent == that.useAllocationSampleEvent
        && includeAgentInternalStacks == that.includeAgentInternalStacks
        && includeJvmInternalStacks == that.includeJvmInternalStacks
        && tracingStacksOnly == that.tracingStacksOnly
        && stackDepth == that.stackDepth
        && keepFiles == that.keepFiles
        && Objects.equals(ingestUrl, that.ingestUrl)
        && Objects.equals(otlpProtocol, that.otlpProtocol)
        && Objects.equals(memoryEventRate, that.memoryEventRate)
        && Objects.equals(callStackInterval, that.callStackInterval)
        && Objects.equals(profilerDirectory, that.profilerDirectory)
        && Objects.equals(recordingDuration, that.recordingDuration)
        && Objects.equals(configProperties, that.configProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        enabled,
        ingestUrl,
        otlpProtocol,
        memoryEnabled,
        memoryEventRateLimitEnabled,
        memoryEventRate,
        useAllocationSampleEvent,
        callStackInterval,
        includeAgentInternalStacks,
        includeJvmInternalStacks,
        tracingStacksOnly,
        stackDepth,
        keepFiles,
        profilerDirectory,
        recordingDuration,
        configProperties);
  }

  public static int getJavaVersion() {
    String javaSpecVersion = System.getProperty("java.specification.version");
    if ("1.8".equals(javaSpecVersion)) {
      return 8;
    }
    return Integer.parseInt(javaSpecVersion);
  }

  public static class Builder {
    private boolean enabled;
    @Nullable private String ingestUrl;
    @Nullable private String otlpProtocol;
    private boolean memoryEnabled;
    private boolean memoryEventRateLimitEnabled = true;
    private String memoryEventRate = "150/s";
    private boolean useAllocationSampleEvent;
    private Duration callStackInterval = DEFAULT_CALL_STACK_INTERVAL;
    private boolean includeAgentInternalStacks;
    private boolean includeJvmInternalStacks;
    private boolean tracingStacksOnly;
    private int stackDepth = 1024;
    private boolean keepFiles;
    private String profilerDirectory = DEFAULT_PROFILER_DIRECTORY;
    private Duration recordingDuration = DEFAULT_RECORDING_DURATION;
    @Nullable private Object configProperties;

    private Builder() {}

    private Builder(ProfilerConfiguration config) {
      enabled = config.enabled;
      ingestUrl = config.ingestUrl;
      otlpProtocol = config.otlpProtocol;
      memoryEnabled = config.memoryEnabled;
      memoryEventRateLimitEnabled = config.memoryEventRateLimitEnabled;
      memoryEventRate = config.memoryEventRate;
      useAllocationSampleEvent = config.useAllocationSampleEvent;
      callStackInterval = config.callStackInterval;
      includeAgentInternalStacks = config.includeAgentInternalStacks;
      includeJvmInternalStacks = config.includeJvmInternalStacks;
      tracingStacksOnly = config.tracingStacksOnly;
      stackDepth = config.stackDepth;
      keepFiles = config.keepFiles;
      profilerDirectory = config.profilerDirectory;
      recordingDuration = config.recordingDuration;
      configProperties = config.configProperties;
    }

    public ProfilerConfiguration build() {
      return new ProfilerConfiguration(this);
    }

    public Builder setEnabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder setIngestUrl(@Nullable String ingestUrl) {
      this.ingestUrl = ingestUrl;
      return this;
    }

    public Builder setOtlpProtocol(@Nullable String otlpProtocol) {
      this.otlpProtocol = otlpProtocol;
      return this;
    }

    public Builder setMemoryEnabled(boolean memoryEnabled) {
      this.memoryEnabled = memoryEnabled;
      return this;
    }

    public Builder setMemoryEventRateLimitEnabled(boolean memoryEventRateLimitEnabled) {
      this.memoryEventRateLimitEnabled = memoryEventRateLimitEnabled;
      return this;
    }

    public Builder setMemoryEventRate(String memoryEventRate) {
      this.memoryEventRate = Objects.requireNonNull(memoryEventRate);
      return this;
    }

    public Builder setUseAllocationSampleEvent(boolean useAllocationSampleEvent) {
      this.useAllocationSampleEvent = useAllocationSampleEvent;
      return this;
    }

    public Builder setCallStackInterval(Duration callStackInterval) {
      this.callStackInterval = Objects.requireNonNull(callStackInterval);
      return this;
    }

    public Builder setIncludeAgentInternalStacks(boolean includeAgentInternalStacks) {
      this.includeAgentInternalStacks = includeAgentInternalStacks;
      return this;
    }

    public Builder setIncludeJvmInternalStacks(boolean includeJvmInternalStacks) {
      this.includeJvmInternalStacks = includeJvmInternalStacks;
      return this;
    }

    public Builder setTracingStacksOnly(boolean tracingStacksOnly) {
      this.tracingStacksOnly = tracingStacksOnly;
      return this;
    }

    public Builder setStackDepth(int stackDepth) {
      this.stackDepth = stackDepth;
      return this;
    }

    public Builder setKeepFiles(boolean keepFiles) {
      this.keepFiles = keepFiles;
      return this;
    }

    public Builder setProfilerDirectory(String profilerDirectory) {
      this.profilerDirectory = Objects.requireNonNull(profilerDirectory);
      return this;
    }

    public Builder setRecordingDuration(Duration recordingDuration) {
      this.recordingDuration = Objects.requireNonNull(recordingDuration);
      return this;
    }

    public Builder setConfigProperties(@Nullable Object configProperties) {
      this.configProperties = configProperties;
      return this;
    }
  }
}
