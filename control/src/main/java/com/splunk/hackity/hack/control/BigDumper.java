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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.function.Consumer;

public class BigDumper {

  private static final java.util.logging.Logger LOGGER =
      java.util.logging.Logger.getLogger(BigDumper.class.getName());

  private final ThreadMXBean threadMXBean;
  private final Consumer<ThreadInfo[]> threadDumpExporter;

  public BigDumper(Consumer<ThreadInfo[]> threadDumpExporter) {
    this.threadMXBean = ManagementFactory.getThreadMXBean();
    this.threadDumpExporter = threadDumpExporter;
  }

  public void dump() {
    LOGGER.fine("Taking a thread dump");
    ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
    threadDumpExporter.accept(threadInfos);
  }
}
