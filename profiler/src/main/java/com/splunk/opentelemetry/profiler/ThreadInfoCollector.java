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

import com.google.common.annotations.VisibleForTesting;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Collects thread stack and optional lock information from the JVM. */
public class ThreadInfoCollector {
  private static final Logger logger = Logger.getLogger(ThreadInfoCollector.class.getName());

  private final ThreadMXBean threadMXBean;
  private final boolean locksEnabled;

  public ThreadInfoCollector(boolean locksEnabled) {
    this(ManagementFactory.getThreadMXBean(), locksEnabled);
  }

  @VisibleForTesting
  public ThreadInfoCollector(ThreadMXBean threadMXBean) {
    this(threadMXBean, false);
  }

  @VisibleForTesting
  public ThreadInfoCollector(ThreadMXBean threadMXBean, boolean locksEnabled) {
    this.threadMXBean = threadMXBean;
    this.locksEnabled = locksEnabled;
  }

  public ThreadInfo getThreadInfo(long threadId) {
    try {
      ThreadInfo[] threadInfos = collectThreadInfo(new long[] {threadId});
      return threadInfos.length == 0 ? null : threadInfos[0];
    } catch (Exception e) {
      logger.log(Level.SEVERE, e, () -> "Error taking callstack sample for thread id " + threadId);
    }
    return null;
  }

  public ThreadInfo[] getThreadInfo(Collection<Long> threadIds) {
    try {
      long[] threadIdArray = threadIds.stream().mapToLong(Long::longValue).toArray();
      return collectThreadInfo(threadIdArray);
    } catch (Exception e) {
      logger.log(
          Level.SEVERE,
          e,
          () -> "Error taking callstack samples for thread ids [" + threadIds + "]");
    }
    return new ThreadInfo[0];
  }

  private ThreadInfo[] collectThreadInfo(long[] threadIds) {
    return threadMXBean.getThreadInfo(
        threadIds,
        locksEnabled && threadMXBean.isObjectMonitorUsageSupported(),
        locksEnabled && threadMXBean.isSynchronizerUsageSupported());
  }
}
