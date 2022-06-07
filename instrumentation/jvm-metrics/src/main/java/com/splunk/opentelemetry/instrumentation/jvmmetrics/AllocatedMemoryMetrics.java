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

package com.splunk.opentelemetry.instrumentation.jvmmetrics;

import com.sun.management.ThreadMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AllocatedMemoryMetrics implements MeterBinder {
  public static final String METRIC_NAME = "process.runtime.jvm.memory.allocated";
  private final boolean hasComSunThreadMXBean = hasComSunThreadMXBean();

  @Override
  public void bindTo(MeterRegistry registry) {
    if (!hasComSunThreadMXBean) {
      return;
    }

    if (!isThreadAllocatedMemoryEnabled()) {
      return;
    }

    AllocationTracker allocationTracker = new AllocationTracker();
    Gauge.builder(METRIC_NAME, allocationTracker::getCumulativeAllocationTotal)
        .description("Approximate sum of heap allocations")
        .baseUnit(BaseUnits.BYTES)
        .tag("type", "heap")
        .register(registry);
  }

  private static boolean hasComSunThreadMXBean() {
    try {
      Class.forName(
          "com.sun.management.ThreadMXBean", false, AllocatedMemoryMetrics.class.getClassLoader());
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private static boolean isThreadAllocatedMemoryEnabled() {
    ThreadMXBean threadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();

    try {
      threadBean.getAllThreadIds();
      return threadBean.isThreadAllocatedMemorySupported()
          && threadBean.isThreadAllocatedMemoryEnabled();
    } catch (Error error) {
      // An error will be thrown for unsupported operations
      // e.g. SubstrateVM does not support getAllThreadIds
      return false;
    }
  }

  private static class AllocationTracker {
    private final ThreadMXBean threadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    // map from thread id -> last measured allocated bytes
    private final Map<Long, Long> allocatedByThread = new HashMap<>();
    // allocated bytes by threads that have terminated
    private long allocatedByDeadThreads = 0;

    long getCumulativeAllocationTotal() {
      Set<Long> threads = new HashSet<>();
      long allocatedByLiveThreads = 0;
      long[] threadIds = threadBean.getAllThreadIds();
      for (long threadId : threadIds) {
        long size = threadBean.getThreadAllocatedBytes(threadId);
        if (size != -1) {
          threads.add(threadId);
          allocatedByThread.put(threadId, size);
          allocatedByLiveThreads += size;
        }
      }

      // find dead threads
      Set<Long> deadThreads = new HashSet<>(allocatedByThread.keySet());
      deadThreads.removeAll(threads);
      for (Long threadId : deadThreads) {
        // remove dead thread and accumulate its allocated bytes
        long size = allocatedByThread.remove(threadId);
        allocatedByDeadThreads += size;
      }

      return allocatedByLiveThreads + allocatedByDeadThreads;
    }
  }
}
