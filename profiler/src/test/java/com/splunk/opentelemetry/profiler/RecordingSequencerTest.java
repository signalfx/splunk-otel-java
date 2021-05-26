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

package com.splunk.opentelemetry.profiler;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordingSequencerTest {

  Duration duration = Duration.ofMillis(10);

  @Mock JfrRecorder recorder;
  @Mock RecordingStartPredicate predicate;

  @Test
  void testHappyPathRecordingsAreScheduled() throws Exception {
    CountDownLatch latch = new CountDownLatch(3);
    when(predicate.canStart()).thenReturn(true);
    doAnswer(
            invocation -> {
              latch.countDown();
              return null;
            })
        .when(recorder)
        .flushSnapshot();

    RecordingSequencer sequencer = buildSequencer();
    sequencer.start();
    assertTrue(latch.await(5, SECONDS));
  }

  @Test
  void testRecordingCannotStart() throws Exception {
    CountDownLatch latch = new CountDownLatch(3);
    doAnswer(
            invocation -> {
              latch.countDown();
              return false;
            })
        .when(predicate)
        .canStart();

    RecordingSequencer sequencer = buildSequencer();
    sequencer.start();
    assertTrue(latch.await(5, SECONDS));
    verifyNoInteractions(recorder);
  }

  @Test
  void testRecoversAfterSkipping() throws Exception {
    CountDownLatch latch = new CountDownLatch(3);
    when(predicate.canStart()).thenReturn(true);
    when(predicate.canStart()).thenReturn(true);
    when(predicate.canStart()).thenReturn(false);
    when(predicate.canStart()).thenReturn(false);
    when(predicate.canStart()).thenReturn(true);
    doAnswer(
            invocation -> {
              latch.countDown();
              return null;
            })
        .when(recorder)
        .flushSnapshot();

    RecordingSequencer sequencer = buildSequencer();
    sequencer.start();
    assertTrue(latch.await(5, SECONDS));
    verify(predicate, times(3)).canStart();
  }

  private RecordingSequencer buildSequencer() {
    return RecordingSequencer.builder()
        .recordingStartPredicate(predicate)
        .recordingDuration(duration)
        .recorder(recorder)
        .build();
  }
}
