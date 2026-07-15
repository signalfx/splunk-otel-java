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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CommandDispatcherImplTest {

  @Test
  void dispatchesThreadDumpWithJobIdCountAndInterval() {
    BigDumper threadDumper = mock(BigDumper.class);
    when(threadDumper.startPeriodicDumper("job-123", 3, Duration.ofMillis(250))).thenReturn(true);
    CommandDispatcher dispatcher = new CommandDispatcherImpl(threadDumper);

    dispatcher.dispatch("text/plain", "thread.dump\njob-123\n3\n250");

    verify(threadDumper).startPeriodicDumper("job-123", 3, Duration.ofMillis(250));
  }

  @Test
  void usesThreadDumpDefaults() {
    BigDumper threadDumper = mock(BigDumper.class);
    when(threadDumper.startPeriodicDumper("job-123", 1, Duration.ofMillis(1000))).thenReturn(true);
    CommandDispatcher dispatcher = new CommandDispatcherImpl(threadDumper);

    dispatcher.dispatch("text/plain", "thread.dump\r\njob-123\r\n");

    verify(threadDumper).startPeriodicDumper("job-123", 1, Duration.ofMillis(1000));
  }

  @Test
  void rejectsMissingJobId() {
    BigDumper threadDumper = mock(BigDumper.class);
    CommandDispatcher dispatcher = new CommandDispatcherImpl(threadDumper);

    dispatcher.dispatch("text/plain", "thread.dump");

    verifyNoInteractions(threadDumper);
  }

  @Test
  void rejectsInvalidCountAndIntervalWithoutThrowing() {
    BigDumper threadDumper = mock(BigDumper.class);
    CommandDispatcher dispatcher = new CommandDispatcherImpl(threadDumper);

    assertDoesNotThrow(() -> dispatcher.dispatch("text/plain", "thread.dump\njob-123\n0\n1000"));
    assertDoesNotThrow(
        () -> dispatcher.dispatch("text/plain", "thread.dump\njob-123\n1\nnot-a-number"));

    verifyNoInteractions(threadDumper);
  }

  @Test
  void containsThreadDumpFailures() {
    BigDumper threadDumper = mock(BigDumper.class);
    when(threadDumper.startPeriodicDumper("job-123", 1, Duration.ofMillis(1000)))
        .thenThrow(new IllegalStateException("export failed"));
    CommandDispatcher dispatcher = new CommandDispatcherImpl(threadDumper);

    assertDoesNotThrow(() -> dispatcher.dispatch("text/plain", "thread.dump\njob-123"));
  }
}
