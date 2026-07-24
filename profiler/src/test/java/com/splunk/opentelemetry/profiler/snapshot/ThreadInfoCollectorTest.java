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

package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ThreadInfoCollectorTest {

  @Test
  void requestsLockInformationForCollectedThreads() {
    ThreadMXBean threadMXBean = mock(ThreadMXBean.class);
    ThreadInfo threadInfo = mock(ThreadInfo.class);
    when(threadMXBean.isObjectMonitorUsageSupported()).thenReturn(true);
    when(threadMXBean.isSynchronizerUsageSupported()).thenReturn(true);
    when(threadMXBean.getThreadInfo(any(long[].class), eq(true), eq(true)))
        .thenReturn(new ThreadInfo[] {threadInfo});
    ThreadInfoCollector collector = new ThreadInfoCollector(threadMXBean);

    ThreadInfo[] result = collector.getThreadInfo(List.of(17L));

    ArgumentCaptor<long[]> threadIds = ArgumentCaptor.forClass(long[].class);
    verify(threadMXBean).getThreadInfo(threadIds.capture(), eq(true), eq(true));
    assertThat(threadIds.getValue()).containsExactly(17L);
    assertThat(result).containsExactly(threadInfo);
  }
}
