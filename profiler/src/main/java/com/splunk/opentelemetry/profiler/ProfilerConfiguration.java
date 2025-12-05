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

import java.time.Duration;

public interface ProfilerConfiguration {
  boolean HAS_OBJECT_ALLOCATION_SAMPLE_EVENT = getJavaVersion() >= 16;

  boolean isEnabled();

  void log();

  String getIngestUrl();

  String getOtlpProtocol();

  boolean getMemoryEnabled();

  boolean getMemoryEventRateLimitEnabled();

  String getMemoryEventRate();

  boolean getUseAllocationSampleEvent();

  Duration getCallStackInterval();

  boolean getIncludeAgentInternalStacks();

  boolean getIncludeJvmInternalStacks();

  boolean getTracingStacksOnly();

  int getStackDepth();

  boolean getKeepFiles();

  String getProfilerDirectory();

  Duration getRecordingDuration();

  Object getConfigProperties();

  static int getJavaVersion() {
    String javaSpecVersion = System.getProperty("java.specification.version");
    if ("1.8".equals(javaSpecVersion)) {
      return 8;
    }
    return Integer.parseInt(javaSpecVersion);
  }
}
