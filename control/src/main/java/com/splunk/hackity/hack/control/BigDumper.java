package com.splunk.hackity.hack.control;

import io.opentelemetry.api.logs.Logger;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class BigDumper {

  private final static java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(BigDumper.class.getName());

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
