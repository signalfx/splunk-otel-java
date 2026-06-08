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

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.time.Duration;
import java.util.Map;

class TestProfilingConfig implements ProfilerConfiguration {

  boolean logCalled;
  String profilerDirectory;
  boolean keepFiles;
  int stackDepth = 1024;
  Duration recordingDuration = Duration.ofDays(1);
  Duration callStackInterval = Duration.ofSeconds(10);
  boolean includeAgentInternalStacks;
  boolean includeJvmInternalStacks;
  boolean tracingStacksOnly;
  boolean memoryEnabled;
  boolean memoryEventRateLimitEnabled = true;
  String memoryEventRate = "150/s";
  boolean useAllocationSampleEvent;
  Object configProperties =
      DefaultConfigProperties.createFromMap(
          Map.of(
              "otel.exporter.otlp.protocol",
              "http/protobuf",
              "otel.exporter.otlp.endpoint",
              "http://localhost:4318"));

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void log() {
    logCalled = true;
  }

  @Override
  public String getIngestUrl() {
    return "http://localhost:4318/v1/logs";
  }

  @Override
  public String getOtlpProtocol() {
    return "http/protobuf";
  }

  @Override
  public boolean getMemoryEnabled() {
    return memoryEnabled;
  }

  @Override
  public boolean getMemoryEventRateLimitEnabled() {
    return memoryEventRateLimitEnabled;
  }

  @Override
  public String getMemoryEventRate() {
    return memoryEventRate;
  }

  @Override
  public boolean getUseAllocationSampleEvent() {
    return useAllocationSampleEvent;
  }

  @Override
  public Duration getCallStackInterval() {
    return callStackInterval;
  }

  @Override
  public boolean getIncludeAgentInternalStacks() {
    return includeAgentInternalStacks;
  }

  @Override
  public boolean getIncludeJvmInternalStacks() {
    return includeJvmInternalStacks;
  }

  @Override
  public boolean getTracingStacksOnly() {
    return tracingStacksOnly;
  }

  @Override
  public int getStackDepth() {
    return stackDepth;
  }

  @Override
  public boolean getKeepFiles() {
    return keepFiles;
  }

  @Override
  public String getProfilerDirectory() {
    return profilerDirectory;
  }

  @Override
  public Duration getRecordingDuration() {
    return recordingDuration;
  }

  @Override
  public Object getConfigProperties() {
    return configProperties;
  }
}
