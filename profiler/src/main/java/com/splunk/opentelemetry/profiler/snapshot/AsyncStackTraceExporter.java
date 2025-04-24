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

package com.splunk.opentelemetry.profiler.snapshot;

import com.splunk.opentelemetry.profiler.InstrumentationSource;
import com.splunk.opentelemetry.profiler.exporter.CpuEventExporter;
import com.splunk.opentelemetry.profiler.exporter.PprofCpuEventExporter;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import io.opentelemetry.api.logs.Logger;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

class AsyncStackTraceExporter implements StackTraceExporter {
  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(AsyncStackTraceExporter.class.getName());

  private final ExecutorService executor =
      HelpfulExecutors.newSingleThreadExecutor("async-stack-trace-exporter");
  private final Logger otelLogger;
  private final int maxDepth;

  AsyncStackTraceExporter(Logger logger, int maxDepth) {
    this.otelLogger = logger;
    this.maxDepth = maxDepth;
  }

  @Override
  public void export(List<StackTrace> stackTraces) {
    executor.submit(pprofExporter(otelLogger, stackTraces));
  }

  private Runnable pprofExporter(Logger otelLogger, List<StackTrace> stackTraces) {
    return () -> {
      try {
        CpuEventExporter cpuEventExporter =
            PprofCpuEventExporter.builder()
                .otelLogger(otelLogger)
                .stackDepth(maxDepth)
                .instrumentationSource(InstrumentationSource.SNAPSHOT)
                .build();

        for (StackTrace stackTrace : stackTraces) {
          cpuEventExporter.export(
              stackTrace.getThreadId(),
              stackTrace.getThreadName(),
              stackTrace.getThreadState(),
              stackTrace.getStackFrames(),
              stackTrace.getTimestamp(),
              stackTrace.getTraceId(),
              stackTrace.getSpanId(),
              stackTrace.getDuration());
        }
        cpuEventExporter.flush();
      } catch (Exception e) {
        logger.log(Level.SEVERE, "An exception was thrown while exporting profiling snapshots.", e);
      }
    };
  }
}
