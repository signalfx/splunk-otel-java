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

package com.splunk.opentelemetry.profiler.exporter;

import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import java.time.Duration;
import java.time.Instant;

public interface CpuEventExporter {

  void export(StackToSpanLinkage stackToSpanLinkage);

  default void export(
      long threadId,
      String threadName,
      Thread.State threadState,
      StackTraceElement[] stackTrace,
      Instant eventTime,
      String traceId,
      String spanId,
      Duration duration) {}

  default void flush() {}
}
