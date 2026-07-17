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

package com.splunk.opamp.remotecontrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.perftools.profiles.ProfileProto.Label;
import com.google.perftools.profiles.ProfileProto.Profile;
import com.google.perftools.profiles.ProfileProto.Sample;
import com.splunk.opentelemetry.profiler.pprof.Pprof;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;

class PprofThreadDumpMarshalerTest {

  @Test
  void emitsBackendThreadDumpLabels() throws Exception {
    StackTraceElement frame = new StackTraceElement("example.Worker", "run", "Worker.java", 42);
    ThreadInfo thread = mock(ThreadInfo.class);
    when(thread.getThreadId()).thenReturn(17L);
    when(thread.getThreadName()).thenReturn("worker-17");
    when(thread.getThreadState()).thenReturn(Thread.State.BLOCKED);
    when(thread.getLockInfo()).thenReturn(new LockInfo("example.WaitingLock", 0x12ab));
    when(thread.getLockOwnerName()).thenReturn("lock-owner");
    when(thread.getLockedMonitors())
        .thenReturn(new MonitorInfo[] {new MonitorInfo("example.Monitor", 0x23bc, 0, frame)});
    when(thread.getLockedSynchronizers())
        .thenReturn(new LockInfo[] {new LockInfo("example.Synchronizer", 0x34cd)});
    when(thread.getStackTrace()).thenReturn(new StackTraceElement[] {frame});

    Pprof pprof = new PprofThreadDumpMarshaler().marshal("job-123", new ThreadInfo[] {thread});
    Profile profile = deserialize(pprof);
    Map<String, Object> labels = labels(profile.getSample(0), profile);

    assertThat(labels)
        .containsEntry("thread.id", 17L)
        .containsEntry("thread.name", "worker-17")
        .containsEntry("thread.state", "BLOCKED")
        .containsEntry("lock.waiting_on", "example.WaitingLock@12ab")
        .containsEntry("lock.owner_thread", "lock-owner")
        .containsEntry("lock.held.0", "example.Monitor@23bc")
        .containsEntry("lock.held.1", "example.Synchronizer@34cd")
        .containsEntry("profiling.job.id", "job-123")
        .doesNotContainKeys(
            "thread.lock.name",
            "thread.lock.info",
            "thread.locked.monitor",
            "thread.locked.synchronizer",
            "source.event.period");
  }

  @Test
  void omitsUnavailableOptionalLockLabels() throws Exception {
    ThreadInfo thread = mock(ThreadInfo.class);
    when(thread.getThreadId()).thenReturn(1L);
    when(thread.getThreadName()).thenReturn("unlocked");
    when(thread.getThreadState()).thenReturn(Thread.State.RUNNABLE);
    when(thread.getLockedMonitors()).thenReturn(new MonitorInfo[0]);
    when(thread.getLockedSynchronizers()).thenReturn(new LockInfo[0]);
    when(thread.getStackTrace()).thenReturn(new StackTraceElement[0]);

    Pprof pprof = new PprofThreadDumpMarshaler().marshal("job-456", new ThreadInfo[] {thread});
    Profile profile = deserialize(pprof);
    Map<String, Object> labels = labels(profile.getSample(0), profile);

    assertThat(labels)
        .containsEntry("profiling.job.id", "job-456")
        .doesNotContainKeys("lock.waiting_on", "lock.owner_thread", "lock.held.0");
  }

  private static Profile deserialize(Pprof pprof) throws IOException {
    byte[] gzipBytes = Base64.getDecoder().decode(pprof.serialize());
    try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(gzipBytes))) {
      return Profile.parseFrom(input);
    }
  }

  private static Map<String, Object> labels(Sample sample, Profile profile) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Label label : sample.getLabelList()) {
      String key = profile.getStringTable((int) label.getKey());
      Object value =
          label.getStr() == 0 ? label.getNum() : profile.getStringTable((int) label.getStr());
      result.put(key, value);
    }
    return result;
  }
}
