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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.exporter.PprofLogDataExporter;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import org.junit.jupiter.api.Test;

class PprofThreadDumpExporterTest {

  @Test
  void delegatesSerializedProfileToLogDataExporter() {
    PprofLogDataExporter logDataExporter = mock(PprofLogDataExporter.class);
    ThreadInfo thread = mock(ThreadInfo.class);
    when(thread.getThreadId()).thenReturn(1L);
    when(thread.getThreadName()).thenReturn("worker");
    when(thread.getThreadState()).thenReturn(Thread.State.RUNNABLE);
    when(thread.getLockedMonitors()).thenReturn(new MonitorInfo[0]);
    when(thread.getLockedSynchronizers()).thenReturn(new LockInfo[0]);
    when(thread.getStackTrace())
        .thenReturn(
            new StackTraceElement[] {
              new StackTraceElement("example.Worker", "run", "Worker.java", 42)
            });

    new PprofThreadDumpExporter(logDataExporter).export("job-123", new ThreadInfo[] {thread});

    verify(logDataExporter).export(any(byte[].class), eq(1));
  }
}
