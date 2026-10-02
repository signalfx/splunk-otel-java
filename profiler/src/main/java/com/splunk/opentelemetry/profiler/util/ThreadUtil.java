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

package com.splunk.opentelemetry.profiler.util;

public class ThreadUtil {

  private static final boolean HAS_VIRTUAL_THREAD = hasVirtualThread();
  private static final boolean HAS_THREAD_ID = hasThreadId();

  // Thread.threadId() is only available since Java 19; getId() is needed for Java 8
  // compatibility.
  @SuppressWarnings("deprecation")
  public static long getThreadId(Thread thread) {
    if (!HAS_VIRTUAL_THREAD) {
      return thread.getId();
    }
    // disable intellij warning that this method was added in a newer version than what we are
    // targeting
    // noinspection Since15
    return thread.threadId();
  }

  public static boolean isVirtual(Thread thread) {
    if (!HAS_VIRTUAL_THREAD) {
      return false;
    }
    // disable intellij warning that this method was added in a newer version than what we are
    // targeting
    // noinspection Since15
    return thread.isVirtual();
  }

  private static boolean hasVirtualThread() {
    try {
      Thread.class.getMethod("isVirtual");
      return true;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  private static boolean hasThreadId() {
    try {
      Thread.class.getMethod("threadId");
      return true;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }
}
