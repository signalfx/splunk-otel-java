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

import static com.splunk.opentelemetry.profiler.util.Runnables.logUncaught;

import com.splunk.opentelemetry.profiler.exporter.CpuEventExporter;
import com.splunk.opentelemetry.profiler.exporter.ThreadData;
import com.splunk.opentelemetry.profiler.threaddump.StackTraceFilter;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import com.splunk.opentelemetry.profiler.util.ThreadUtil;
import io.opentelemetry.api.trace.SpanContext;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** Cpu stack collector that does not use jfr. */
class JavaProfiler {
  private final ScheduledExecutorService scheduler =
      HelpfulExecutors.newSingleThreadedScheduledExecutor("Splunk CPU Profiler");
  private final ProfilerConfiguration config;
  private final CpuEventExporter cpuEventExporter;
  private final StackTraceFilter stackTraceFilter;
  private final ProfilerContextStorage contextStorage;

  JavaProfiler(
      ProfilerConfiguration config,
      CpuEventExporter cpuEventExporter,
      StackTraceFilter stackTraceFilter,
      ProfilerContextStorage contextStorage) {
    this.config = config;
    this.cpuEventExporter = cpuEventExporter;
    this.stackTraceFilter = stackTraceFilter;
    this.contextStorage = contextStorage;
  }

  void start() {
    boolean onlyTracingSpans = config.getTracingStacksOnly();
    boolean locksEnabled = config.getLocksEnabled();
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    Runnable profiler =
        () -> {
          Instant now = Instant.now();

          // capture the state before taking thread dumps
          Map<Thread, SpanContext> activeContext =
              StreamSupport.stream(contextStorage.getActiveContextMap().spliterator(), false)
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

          List<ThreadData> threadDataList = new ArrayList<>(activeContext.size());
          // first capture virtual threads that have an active span, ThreadMXBean does not report
          // virtual threads
          for (Thread thread : activeContext.keySet()) {
            if (ThreadUtil.isVirtual(thread)) {
              threadDataList.add(ThreadData.from(thread));
            }
          }

          // capture platform threads along with lock info
          ThreadInfo[] threadInfos =
              threadMXBean.dumpAllThreads(
                  locksEnabled && threadMXBean.isObjectMonitorUsageSupported(),
                  locksEnabled && threadMXBean.isSynchronizerUsageSupported());
          for (ThreadInfo threadInfo : threadInfos) {
            threadDataList.add(ThreadData.from(threadInfo));
          }

          // construct a mapping from thread id -> span context
          Map<Long, SpanContext> threadIdContexts =
              activeContext.entrySet().stream()
                  .collect(
                      Collectors.toMap(
                          entry -> ThreadUtil.getThreadId(entry.getKey()), Map.Entry::getValue));

          for (ThreadData threadData : threadDataList) {
            if (!stackTraceFilter.test(threadData.getThreadName(), threadData.getStackTrace())) {
              continue;
            }

            SpanContext spanContext = threadIdContexts.get(threadData.getThreadId());
            if (onlyTracingSpans && (spanContext == null || !spanContext.isValid())) {
              continue;
            }

            cpuEventExporter.export(
                threadData,
                now,
                spanContext != null ? spanContext.getTraceId() : null,
                spanContext != null ? spanContext.getSpanId() : null,
                config.getCallStackInterval());
          }
          cpuEventExporter.flush();
        };
    long period = config.getCallStackInterval().toMillis();
    scheduler.scheduleAtFixedRate(logUncaught(profiler), period, period, TimeUnit.MILLISECONDS);
  }

  void stop() {
    scheduler.shutdown();
  }
}
