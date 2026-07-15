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
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_TIME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_STATE;

import com.google.perftools.profiles.ProfileProto.Sample;
import com.splunk.opentelemetry.profiler.ThreadDumpProcessor;
import com.splunk.opentelemetry.profiler.pprof.Pprof;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

/** Marshals a {@link ThreadInfo} snapshot into the pprof shape used by profiling call stacks. */
public class PprofThreadDumpMarshaler {
  static final String LOCK_WAITING_ON = "lock.waiting_on";
  static final String LOCK_OWNER_THREAD = "lock.owner_thread";
  static final String LOCK_HELD_PREFIX = "lock.held.";
  static final String PROFILING_JOB_ID = "profiling.job.id";

  public Pprof marshal(String jobId, ThreadInfo[] threadInfos) {
    Pprof pprof = new Pprof();
    long eventTime = System.currentTimeMillis();

    for (ThreadInfo threadInfo : threadInfos) {
      if (threadInfo != null) {
        addThread(jobId, pprof, threadInfo, eventTime);
      }
    }
    return pprof;
  }

  private static void addThread(String jobId, Pprof pprof, ThreadInfo threadInfo, long eventTime) {
    Sample.Builder sample = Sample.newBuilder();
    pprof.addLabel(sample, THREAD_ID, threadInfo.getThreadId());
    pprof.addLabel(sample, THREAD_NAME, threadInfo.getThreadName());
    pprof.addLabel(sample, THREAD_STATE, threadInfo.getThreadState().name());
    pprof.addLabel(sample, SOURCE_EVENT_NAME, ThreadDumpProcessor.EVENT_NAME);
    pprof.addLabel(sample, SOURCE_EVENT_TIME, eventTime);
    pprof.addLabel(sample, PROFILING_JOB_ID, jobId);

    addLockInfo(pprof, sample, LOCK_WAITING_ON, threadInfo.getLockInfo());
    pprof.addLabel(sample, LOCK_OWNER_THREAD, threadInfo.getLockOwnerName());

    int heldLockIndex = 0;
    for (MonitorInfo monitor : threadInfo.getLockedMonitors()) {
      pprof.addLabel(sample, LOCK_HELD_PREFIX + heldLockIndex++, formatLock(monitor));
    }
    for (LockInfo synchronizer : threadInfo.getLockedSynchronizers()) {
      pprof.addLabel(sample, LOCK_HELD_PREFIX + heldLockIndex++, formatLock(synchronizer));
    }

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

  private static void addLockInfo(
      Pprof pprof, Sample.Builder sample, String labelName, LockInfo lockInfo) {
    if (lockInfo != null) {
      pprof.addLabel(sample, labelName, formatLock(lockInfo));
    }
  }

  private static String formatLock(LockInfo lockInfo) {
    return lockInfo.getClassName() + '@' + Integer.toHexString(lockInfo.getIdentityHashCode());
  }
}
