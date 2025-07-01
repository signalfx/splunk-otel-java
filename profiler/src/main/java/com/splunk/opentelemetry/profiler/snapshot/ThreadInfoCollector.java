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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

class ThreadInfoCollector {
  private static final Logger logger = Logger.getLogger(ThreadInfoCollector.class.getName());

  private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

  ThreadInfo getThreadInfo(long threadId) {
    try {
      return threadMXBean.getThreadInfo(threadId, Integer.MAX_VALUE);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e, () -> "Error taking callstack sample for thread id " + threadId);
    }
    return null;
  }

  ThreadInfo[] getThreadInfo(Collection<Long> threadIds) {
    try {
      long[] threadIdArray = threadIds.stream().mapToLong(Long::longValue).toArray();
      return threadMXBean.getThreadInfo(threadIdArray, Integer.MAX_VALUE);
    } catch (Exception e) {
      logger.log(
          Level.SEVERE,
          e,
          () -> "Error taking callstack samples for thread ids [" + threadIds + "]");
    }
    return new ThreadInfo[0];
  }
}
