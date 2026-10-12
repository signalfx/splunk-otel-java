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

package com.splunk.opentelemetry.profiler.exporter;

import com.splunk.opentelemetry.profiler.util.ThreadUtil;
import java.lang.management.LockInfo;
import java.lang.management.ThreadInfo;

public class ThreadData {

  private final long threadId;
  private final String threadName;
  private final Thread.State threadState;
  private final LockData lock;
  private final String lockOwnerName;
  private final StackTraceElement[] stackTrace;
  private final LockData[] lockedMonitors;
  private final LockData[] lockedSynchronizers;

  private ThreadData(ThreadInfo threadInfo) {
    this.threadId = threadInfo.getThreadId();
    this.threadName = threadInfo.getThreadName();
    this.threadState = threadInfo.getThreadState();
    this.lock = LockData.from(threadInfo.getLockInfo());
    this.lockOwnerName = threadInfo.getLockOwnerName();
    this.stackTrace = threadInfo.getStackTrace();
    this.lockedMonitors = LockData.from(threadInfo.getLockedMonitors());
    this.lockedSynchronizers = LockData.from(threadInfo.getLockedSynchronizers());
  }

  private ThreadData(Thread thread) {
    this.threadId = ThreadUtil.getThreadId(thread);
    this.threadName = thread.getName();
    this.threadState = thread.getState();
    this.lock = null;
    this.lockOwnerName = null;
    this.stackTrace = thread.getStackTrace();
    this.lockedMonitors = new LockData[0];
    this.lockedSynchronizers = new LockData[0];
  }

  public static ThreadData from(ThreadInfo threadInfo) {
    return new ThreadData(threadInfo);
  }

  public static ThreadData from(Thread thread) {
    return new ThreadData(thread);
  }

  public long getThreadId() {
    return threadId;
  }

  public String getThreadName() {
    return threadName;
  }

  public Thread.State getThreadState() {
    return threadState;
  }

  public LockData getLockInfo() {
    return lock;
  }

  public String getLockOwnerName() {
    return lockOwnerName;
  }

  public StackTraceElement[] getStackTrace() {
    return stackTrace;
  }

  public LockData[] getLockedMonitors() {
    return lockedMonitors;
  }

  public LockData[] getLockedSynchronizers() {
    return lockedSynchronizers;
  }

  public static class LockData {

    private final String className;
    private final int identityHashCode;

    private LockData(String className, int identityHashCode) {
      this.className = className;
      this.identityHashCode = identityHashCode;
    }

    private LockData(LockInfo lockInfo) {
      this(lockInfo.getClassName(), lockInfo.getIdentityHashCode());
    }

    static LockData from(LockInfo lockInfo) {
      return lockInfo != null ? new LockData(lockInfo) : null;
    }

    private static LockData[] from(LockInfo[] lockInfos) {
      if (lockInfos == null) {
        return new LockData[0];
      }

      LockData[] lockData = new LockData[lockInfos.length];
      for (int i = 0; i < lockInfos.length; i++) {
        lockData[i] = new LockData(lockInfos[i]);
      }
      return lockData;
    }

    public String getClassName() {
      return className;
    }

    public int getIdentityHashCode() {
      return identityHashCode;
    }
  }
}
