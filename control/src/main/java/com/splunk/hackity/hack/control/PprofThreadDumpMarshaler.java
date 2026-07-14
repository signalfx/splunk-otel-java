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

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_TIME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_BLOCKED_COUNT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_BLOCKED_TIME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_LOCKED_MONITOR;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_LOCKED_SYNCHRONIZER;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_LOCK_INFO;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_LOCK_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_STATE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_WAITED_COUNT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_WAITED_TIME;

import com.google.perftools.profiles.ProfileProto.Sample;
import com.splunk.opentelemetry.profiler.ThreadDumpProcessor;
import com.splunk.opentelemetry.profiler.pprof.Pprof;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

/** Marshals a {@link ThreadInfo} snapshot into the pprof shape used by profiling call stacks. */
public class PprofThreadDumpMarshaler {

  public Pprof marshal(ThreadInfo[] threadInfos) {
    Pprof pprof = new Pprof();
    long eventTime = System.currentTimeMillis();

    for (ThreadInfo threadInfo : threadInfos) {
      if (threadInfo != null) {
        addThread(pprof, threadInfo, eventTime);
      }
    }
    return pprof;
  }

  private static void addThread(Pprof pprof, ThreadInfo threadInfo, long eventTime) {
    Sample.Builder sample = Sample.newBuilder();
    pprof.addLabel(sample, THREAD_ID, threadInfo.getThreadId());
    pprof.addLabel(sample, THREAD_NAME, threadInfo.getThreadName());
    pprof.addLabel(sample, THREAD_STATE, threadInfo.getThreadState().name());
    pprof.addLabel(sample, SOURCE_EVENT_NAME, ThreadDumpProcessor.EVENT_NAME);
    pprof.addLabel(sample, SOURCE_EVENT_PERIOD, 1);
    pprof.addLabel(sample, SOURCE_EVENT_TIME, eventTime);

    pprof.addLabel(sample, THREAD_LOCK_NAME, threadInfo.getLockName());
    addLockInfo(pprof, sample, threadInfo.getLockInfo());
    for (MonitorInfo monitor : threadInfo.getLockedMonitors()) {
      pprof.addLabel(sample, THREAD_LOCKED_MONITOR, formatMonitor(monitor));
    }
    for (LockInfo synchronizer : threadInfo.getLockedSynchronizers()) {
      pprof.addLabel(sample, THREAD_LOCKED_SYNCHRONIZER, formatLock(synchronizer));
    }

    pprof.addLabel(sample, THREAD_WAITED_COUNT, threadInfo.getWaitedCount());
    pprof.addLabel(sample, THREAD_WAITED_TIME, threadInfo.getWaitedTime());
    pprof.addLabel(sample, THREAD_BLOCKED_COUNT, threadInfo.getBlockedCount());
    pprof.addLabel(sample, THREAD_BLOCKED_TIME, threadInfo.getBlockedTime());

    for (StackTraceElement frame : threadInfo.getStackTrace()) {
      String fileName = frame.getFileName() == null ? "unknown" : frame.getFileName();
      sample.addLocationId(
          pprof.getLocationId(
              fileName,
              frame.getClassName(),
              frame.getMethodName(),
              Math.max(frame.getLineNumber(), 0)));
      pprof.incFrameCount();
    }
    pprof.getProfileBuilder().addSample(sample);
  }

  private static void addLockInfo(Pprof pprof, Sample.Builder sample, LockInfo lockInfo) {
    if (lockInfo != null) {
      pprof.addLabel(sample, THREAD_LOCK_INFO, formatLock(lockInfo));
    }
  }

  private static String formatLock(LockInfo lockInfo) {
    return lockInfo.getClassName() + '@' + Integer.toHexString(lockInfo.getIdentityHashCode());
  }

  private static String formatMonitor(MonitorInfo monitor) {
    return formatLock(monitor)
        + ";stackDepth="
        + monitor.getLockedStackDepth()
        + ";stackFrame="
        + monitor.getLockedStackFrame();
  }
}
