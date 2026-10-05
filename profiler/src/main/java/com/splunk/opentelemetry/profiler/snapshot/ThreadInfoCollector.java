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

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.exporter.ThreadData;
import com.splunk.opentelemetry.profiler.util.ThreadUtil;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class primarily exists to provide tests with a seam in which to hook into the stack trace
 * collection process for thread coordination purposes.
 */
class ThreadInfoCollector {
  private static final Logger logger = Logger.getLogger(ThreadInfoCollector.class.getName());

  private final ThreadMXBean threadMXBean;
  private final boolean locksEnabled;

  ThreadInfoCollector(boolean locksEnabled) {
    this(ManagementFactory.getThreadMXBean(), locksEnabled);
  }

  @VisibleForTesting
  ThreadInfoCollector(ThreadMXBean threadMXBean) {
    this(threadMXBean, false);
  }

  @VisibleForTesting
  ThreadInfoCollector(ThreadMXBean threadMXBean, boolean locksEnabled) {
    this.threadMXBean = threadMXBean;
    this.locksEnabled = locksEnabled;
  }

  ThreadData getThreadInfo(Thread thread) {
    if (ThreadUtil.isVirtual(thread)) {
      return ThreadData.from(thread);
    }

    try {
      ThreadInfo[] threadInfos = collectThreadInfo(new long[] {ThreadUtil.getThreadId(thread)});
      return threadInfos.length == 0 ? null : ThreadData.from(threadInfos[0]);
    } catch (Exception e) {
      logger.log(Level.WARNING, e, () -> "Error taking callstack sample for thread " + thread);
    }
    return null;
  }

  Collection<ThreadData> getThreadInfo(Collection<Thread> threads) {
    List<ThreadData> result = new ArrayList<>();
    for (Thread thread : threads) {
      if (ThreadUtil.isVirtual(thread)) {
        result.add(ThreadData.from(thread));
      }
    }

    try {
      long[] threadIdArray = threads.stream().mapToLong(ThreadUtil::getThreadId).toArray();
      ThreadInfo[] threadInfos = collectThreadInfo(threadIdArray);
      for (ThreadInfo threadInfo : threadInfos) {
        if (threadInfo != null) {
          result.add(ThreadData.from(threadInfo));
        }
      }
    } catch (Exception e) {
      logger.log(
          Level.WARNING,
          e,
          () -> "Error taking callstack samples for thread ids [" + threads + "]");
      return Collections.emptyList();
    }

    return result;
  }

  private ThreadInfo[] collectThreadInfo(long[] threadIds) {
    return threadMXBean.getThreadInfo(
        threadIds,
        locksEnabled && threadMXBean.isObjectMonitorUsageSupported(),
        locksEnabled && threadMXBean.isSynchronizerUsageSupported());
  }
}
