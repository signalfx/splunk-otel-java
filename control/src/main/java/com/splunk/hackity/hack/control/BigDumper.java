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

package com.splunk.hackity.hack.control;

import io.opentelemetry.api.logs.Logger;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class BigDumper {

  private static final java.util.logging.Logger LOGGER =
      java.util.logging.Logger.getLogger(BigDumper.class.getName());

  private final Logger otelLogger;

  public BigDumper(Logger otelLogger) {
    this.otelLogger = otelLogger;
  }

  public void dump() {
    LOGGER.fine("Taking a thread dump");
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
    for (ThreadInfo threadInfo : threadInfos) {
      long threadId = threadInfo.getThreadId();
      String threadName = threadInfo.getThreadName();
      String lockName = threadInfo.getLockName();
      long waitedCount = threadInfo.getWaitedCount();
      long waitedTime = threadInfo.getWaitedTime();
      long blockedCount = threadInfo.getBlockedCount();
      long blockedTime = threadInfo.getBlockedTime();
      LockInfo lockInfo = threadInfo.getLockInfo();
      MonitorInfo[] lockedMonitors = threadInfo.getLockedMonitors();
      LockInfo[] lockedSynchronizers = threadInfo.getLockedSynchronizers();
    }
  }
}
